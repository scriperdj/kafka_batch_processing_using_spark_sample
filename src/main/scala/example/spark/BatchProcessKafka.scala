package example.spark

import org.apache.spark.{ SparkConf, SparkContext, _}
import org.apache.spark.sql.{DataFrame, SQLContext, SparkSession}
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.spark.streaming.kafka010.LocationStrategies.PreferConsistent
import org.apache.spark.streaming.kafka010._

import org.apache.kafka.clients.admin.{AdminClient,NewTopic,AdminClientConfig}
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.TopicPartition


import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.Constructor

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.{File, FileInputStream}
import java.util.Properties
import java.time.Instant
import collection.JavaConverters._
import collection.JavaConversions._

object BatchProcessKafka{

  val APP_NAME = "BatchProcessKafka"
  val LOG = LoggerFactory.getLogger(BatchProcessKafka.getClass);

  def main(args: Array[String]): Unit = {
    val file = new FileInputStream(new File(args(0)))
    val yaml = new Yaml(new Constructor(classOf[Config]))
    val config = yaml.load(file).asInstanceOf[Config]

    val conf = new SparkConf().setAppName(APP_NAME)
    val sc = new SparkContext(conf)
    val sparkSession = SparkSession.builder.config(conf).appName(APP_NAME).getOrCreate()
    val sqlContext = sparkSession.sqlContext
    import sparkSession.implicits._

    val gid = APP_NAME + Instant.now.getEpochSecond

    val kafkaParams = scala.collection.immutable.Map[String, Object](
      "bootstrap.servers" -> config.kafkaBrokers,
      "key.deserializer" -> classOf[StringDeserializer],
      "value.deserializer" -> classOf[StringDeserializer],
      "group.id" -> gid,
      "auto.offset.reset" -> "earliest",
      "enable.auto.commit" -> (false: java.lang.Boolean)
    ).asJava

    val KafkaConfig = new Properties()
    KafkaConfig.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, config.kafkaBrokers)
    val adminClient = AdminClient.create(KafkaConfig)

    val consumer = new KafkaConsumer[String,String](kafkaParams)

    val topicPartitions = adminClient
                          .describeTopics(List[String](config.inputTopic).asJava)
                          .all().get().get(config.inputTopic).partitions()

    val offsetRanges = topicPartitions.asScala.map(x =>{
      val topicPartition = new TopicPartition(config.inputTopic, x.partition)
      val startOffset = consumer.beginningOffsets(List[TopicPartition](topicPartition))
                        .values().asScala.toList.get(0)
      val stopOffset = consumer.endOffsets(List[TopicPartition](topicPartition))
                        .values().asScala.toList.get(0)
      OffsetRange(topicPartition,
                  startOffset,
                  stopOffset)
    }).toArray

    val messagesRDD = KafkaUtils.createRDD[String, String](sc, kafkaParams,
                          offsetRanges, PreferConsistent)


  }
}
