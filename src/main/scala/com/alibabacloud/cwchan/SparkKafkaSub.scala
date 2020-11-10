package com.alibabacloud.cwchan

import org.apache.spark.sql.Dataset
import org.apache.spark.sql.Row
import org.apache.spark.sql.SparkSession

import com.typesafe.config.ConfigFactory

object SparkKafkaSub {

  var bootstrapServers: String = null;
  var groupId: String = null;
  var topicName: String = null;

  var kafkaLoginModule: String = null;
  var kafkaUsername: String = null;
  var kafkaPassword: String = null;
  var jassConfigEntity: String = null;

  def loadConfig(): SparkSession = {
    val config = ConfigFactory.load().getConfig("com.alibaba-inc.cwchan");
    val kafkaConfig = config.getConfig("Kafka");
    bootstrapServers = kafkaConfig.getString("bootstrapServers");
    groupId = kafkaConfig.getString("groupId");
    topicName = kafkaConfig.getString("topicName");

    println("running kafka subscriber for " + topicName);

    return SparkApp.sparkSessoin;
  }

  def run(): Unit = {

    val sparkSession = loadConfig();

    val dfStatic = sparkSession
      .read
      .format("jdbc")
      .option("url", "jdbc:postgresql://gp-3ns887x3g7nxa3d4eo.gpdb.rds.aliyuncs.com:3432/sku")
      .option("dbtable", "public.hsistock")
      .option("user", "cwchan")
      .option("password", "@liP@ssw0rd")
      .option("driver", "org.postgresql.Driver")
      .load()
      .selectExpr("symbol as Symbol", "`Company Name` as CompanyName", "`Last Price` as LastPrice", "volume as Volume")

    val dfStream = sparkSession
      .readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", bootstrapServers)
      .option("subscribe", topicName)
      .option("group.id", groupId)
      .load()
      .selectExpr("CAST(key AS STRING) as SourceIP", "split(value, ',')[0] as StockCode", "split(value, ',')[1] as TS")

    val dfJoin = dfStream
      .join(dfStatic, dfStream("StockCode") === dfStatic("Symbol"), "leftouter")
      .selectExpr("SourceIP", "StockCode", "CompanyName", "LastPrice", "TS")

    val query = dfJoin
      .writeStream
      .foreachBatch { (output: Dataset[Row], batchId: Long) => 
        SparkHBaseWriter.open();
        for (r <- output.collect()) {
          SparkHBaseWriter.process(r);
        }
        SparkHBaseWriter.close();
      }
      .start
      .awaitTermination();

  }

}