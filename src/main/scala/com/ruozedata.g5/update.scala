package com.ruozedata.g5

import org.apache.spark.{HashPartitioner, SparkConf}
import org.apache.spark.streaming.{Seconds, StreamingContext}

object update {
  val updateFunc = (iter: Iterator[(String, Seq[Int], Option[Int])]) => {
    //iter.flatMap(it=>Some(it._2.sum + it._3.getOrElse(0)).map(x=>(it._1,x)))
    iter.flatMap{case(x,y,z)=>Some(y.sum + z.getOrElse(0)).map(m=>(x, m))}
  }

  def main(args: Array[String]) {
    val conf = new SparkConf().setMaster("local[2]").setAppName("damo_updateStateByKey")
    val ssc = new StreamingContext(conf, Seconds(20))
    //为了使用updateStateByKey必须开启checkpoint
    ssc.checkpoint("Z://check")
    val lines = ssc.socketTextStream("hadoop001", 9999)
    //reduceByKey 结果不累加
    //val result = lines.flatMap(_.split(" ")).map((_, 1)).reduceByKey(_+_)
    //updateStateByKey结果可以累加但是需要传入一个自定义的累加函数：updateFunc
    val results = lines.flatMap(_.split(" "))
      .map((_,1)).updateStateByKey(updateFunc,
      new HashPartitioner(ssc.sparkContext.defaultParallelism),true)
    results.print()
    ssc.start()
    ssc.awaitTermination()
  }
}
