# Example Spark Application for Batch processing of multi partitioned Kafka topics

This example application reads given Kafka topic & broker details and does below operations
* Get partition & offset details of provided Kafka topics.
* Create DataFrame with the data read.
* **Find Top trending product in each category based on users browsing data.**

The output is not implemented. It just displays the Top 10 Product/Category in logs.

Please read the blog post [in here](http://sathish.me/scala/2018/02/03/batch-processing-of-multi-partitioned-kafka-topics-using-spark-with-example.html) to know about the approach & introduction about the problem.

## Build
```bash
> sbt package
```

## Execution

* Create kafka topic with required number of partition & other configurations
```bash
> kafka-topics --zookeeper localhost:2181 --replication-factor 1 --create --partitions 5 --topic web_stream --config retention.ms=604800000
```
* Generate Sample data for the topic
```bash
> kafka-console-producer --broker-list localhost:9092 --topic web_stream  --property parse.key=true --property key.separator=,
> cus_001,{"product":"PD0021","category": "Books","ts":"1516978415"}
> cus_001,{"product":"PD0022","category": "TV","ts":"1517978415"}
```
* Submit Spark job
```bash
> spark-submit --packages org.apache.kafka:kafka-clients:0.11.0.0,org.apache.spark:spark-streaming-kafka-0-10_2.11:2.2.0 --class example.spark.BatchProcessKafka --master yarn target/scala-2.11/kafka_batch_processing_using_spark_sample_2.11-1.0.jar web_stream localhost:9092
```
