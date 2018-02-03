package example.spark
/**
 * This Spark application finds top Products in each Category from Users browsing data
 *
 */
import org.apache.spark.{ SparkConf, SparkContext, _}
import org.apache.spark.sql.{DataFrame, SQLContext, SparkSession}
import org.apache.spark.sql.expressions._
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.spark.streaming.kafka010.LocationStrategies.PreferConsistent
import org.apache.spark.streaming.kafka010._

import org.apache.kafka.clients.admin.{AdminClient,NewTopic,AdminClientConfig}
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.TopicPartition

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties
import java.time.Instant
import collection.JavaConverters._
import collection.JavaConversions._
import scala.util.parsing.json._
import scala.util.Try

object BatchProcessKafka{

  val APP_NAME = "BatchProcessKafka"
  val LOG = LoggerFactory.getLogger(BatchProcessKafka.getClass);

  def main(args: Array[String]): Unit = {
    val inputTopic = args(0)
    val kafkaBrokers = args(1)

    val conf = new SparkConf().setAppName(APP_NAME)
    val sc = new SparkContext(conf)
    val sparkSession = SparkSession.builder.config(conf).appName(APP_NAME).getOrCreate()
    val sqlContext = sparkSession.sqlContext
    import sparkSession.implicits._

    val gid = APP_NAME + Instant.now.getEpochSecond

    val kafkaParams = scala.collection.immutable.Map[String, Object](
      "bootstrap.servers" -> kafkaBrokers,
      "key.deserializer" -> classOf[StringDeserializer],
      "value.deserializer" -> classOf[StringDeserializer],
      "group.id" -> gid,
      "auto.offset.reset" -> "earliest",
      "enable.auto.commit" -> (false: java.lang.Boolean)
    ).asJava

    val KafkaConfig = new Properties()
    KafkaConfig.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBrokers)
    val adminClient = AdminClient.create(KafkaConfig)

    val consumer = new KafkaConsumer[String,String](kafkaParams)

    // Get list of all partitions of given Topic
    val topicPartitions = adminClient
                          .describeTopics(List[String](inputTopic).asJava)
                          .all().get().get(inputTopic).partitions()

    // Create Array of OffsetRange with topic, partition number, start & end offsets
    val offsetRanges = topicPartitions.asScala.map(x =>{
      val topicPartition = new TopicPartition(inputTopic, x.partition)
      val startOffset = consumer.beginningOffsets(List[TopicPartition](topicPartition))
                        .values().asScala.toList.get(0)
      val stopOffset = consumer.endOffsets(List[TopicPartition](topicPartition))
                        .values().asScala.toList.get(0)
      OffsetRange(topicPartition,
                    startOffset,
                    stopOffset)
    }).toArray

    // Create RDD from provided topic & offset details
    val messagesRDD = KafkaUtils.createRDD[String, String](sc, kafkaParams,
                          offsetRanges, PreferConsistent)

    // Convert to DataFrame with columns "customer_id","product","category","ts"
    val eventsDF = messagesRDD.map(message =>{
      val value = message.value().asInstanceOf[String]
      val key = message.key().asInstanceOf[String]
      val msgValues = JSON.parseFull(value).get.asInstanceOf[Map[String, String]]
      (key,msgValues("product"),msgValues("category"),msgValues("ts").toLong)
    }).toDF("customer_id","product","category","ts")

    // Compute unique Product/Category count
    val productsDF = eventsDF.groupBy("product","category").count()

    productsDF.createOrReplaceTempView("product_category_count")

    // Find Top Products in each Category
    val topProducts = sqlContext.sql("""SELECT x.category,
                                               x.product,
                                               x.count
                                        from product_category_count x
                                        JOIN (
                                          SELECT p.category,
                                                 max(count) AS max_count
                                          FROM product_category_count p
                                          GROUP BY p.category) y
                                        ON y.category = x.category
                                        AND y.max_count = x.count""")
    topProducts.show()

    // Write to DB or send to downstream Kafka (To be implemented)

    sc.stop()
  }
}
