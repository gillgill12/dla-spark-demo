package com.alibabacloud.cwchan

import java.io.File


object SparkApp {


  def main(args: Array[String]): Unit = {

    val threadPub = new Thread {
      override def run {
        // your custom behavior here
        com.alibabacloud.cwchan.SparkKafkaPub.run();
      }
    }

    val threadSub = new Thread {
      override def run {
        // your custom behavior here
        com.alibabacloud.cwchan.SparkKafkaSub.run();
      }
    }

//    threadSub.start
//    Thread.sleep(5000)
    threadPub.start

  }
}