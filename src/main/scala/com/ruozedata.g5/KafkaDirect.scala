package com.ruozedata.g5

import org.apache.log4j.Level
import org.apache.log4j.Logger
import org.apache.spark.SparkConf
import org.apache.spark.streaming.{Seconds, StreamingContext}
import kafka.serializer.StringDecoder
import org.apache.spark.streaming.kafka.KafkaUtils
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.Connection

object KafkaDirect {
  def main(args: Array[String]): Unit = {
    // 减少日志输出
  Logger.getLogger("org.apache.spark").setLevel(Level.ERROR)

  val sparkConf = new SparkConf().setAppName("KafkaDirect").setMaster("local[2]")
  val sparkStreaming = new StreamingContext(sparkConf, Seconds(10))
  // 创建topic名称
  val topic = Set("test")
  // 制定Kafka的broker地址
  val kafkaParams = Map[String, String]("metadata.broker.list" -> "139.198.189.141:9092")
  // 创建DStream，接受kafka数据irectStream[String, String, StringDecoder,StringDecoder](sparkStreaming, kafkaParams, topic)
  val kafkaStream = KafkaUtils.createDirectStream[String, String, StringDecoder, StringDecoder](sparkStreaming, kafkaParams, topic)
  val line = kafkaStream.map(e => {
    new String(e.toString())
  })
  // 获取数据
  val logRDD = kafkaStream.map(_._2)
  // 将数据打印在屏幕
  logRDD.print()
  // 对接受的数据进行分词处理
  val datas = logRDD.map(line => {
    // 1,201.105.101.108,http://mystore.jsp/?productid=1,2017020029,2,1
    val index: Array[String] = line.split(",")
    val ip = index(1);
    (ip, 1)
  })
  // 打印在屏幕
  datas.print()
  // 将数据保存在mysql数据库
  datas.foreachRDD(cs => {
    var conn: Connection = null;
    var ps: PreparedStatement = null;
    try {
      Class.forName("com.mysql.jdbc.Driver").newInstance();
      cs.foreachPartition(f => {
        conn = DriverManager.getConnection(
          "jdbc:mysql://122.152.216.77:3306/ruozedata?useUnicode=true&characterEncoding=utf8",
          "ruoze",
          "123456");
        ps = conn.prepareStatement("insert into result values(?,?)");
        f.foreach(s => {
          ps.setString(1, s._1);
          ps.setInt(2, s._2);
          ps.executeUpdate();
        })
      })
    } catch {
      case t: Throwable => t.printStackTrace() // TODO: handle error
    } finally {
      if (ps != null) {
        ps.close()
      }
      if (conn != null) {
        conn.close();
      }
    }
  })

  sparkStreaming.start()
  sparkStreaming.awaitTermination()
}
}


