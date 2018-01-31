
name := "kafka_batch_processing_using_spark_sample"

version := "1.0"

scalaVersion := "2.11.11"

val sparkVersion = "2.2.0"
val kafkaVersion = "0.11.0.0"

libraryDependencies += "org.apache.spark" % "spark-core_2.11" % sparkVersion

libraryDependencies += "org.apache.spark" % "spark-streaming_2.11" % sparkVersion

libraryDependencies += "org.apache.spark" % "spark-sql_2.11" % sparkVersion

libraryDependencies += "org.apache.kafka" % "kafka-clients" % kafkaVersion

libraryDependencies += "org.apache.commons" % "commons-exec" % "1.3"

libraryDependencies += "org.yaml" % "snakeyaml" % "1.18"

libraryDependencies += "org.apache.spark" % "spark-streaming-kafka-0-10_2.11" % sparkVersion
