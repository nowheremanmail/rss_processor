package com.dag

import java.io.File

import com.dag.news.bo.TempNew
import com.dag.source.RSSSources
import org.apache.flink.api.common.restartstrategy.RestartStrategies
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.api.scala._
import org.apache.flink.streaming.api.environment.CheckpointConfig.ExternalizedCheckpointCleanup
import org.apache.flink.streaming.api.scala.{DataStream, StreamExecutionEnvironment}
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.api.common.operators.Order
object AnalyticsJob {
  def main(args: Array[String]) {
    def func(json: String): TempNew= JacksonWrapper.deserialize[TempNew](json)

    val params = ParameterTool.fromArgs(args)
    val env = ExecutionEnvironment.getExecutionEnvironment
    env.getConfig.setGlobalJobParameters(params)

    env.setParallelism(1)
    env.setRestartStrategy(RestartStrategies.noRestart())
    env.getConfig.disableSysoutLogging

    def getListOfFiles(dir: String):List[File] = {
      val d = new File(dir)
      if (d.exists && d.isDirectory) {
        d.listFiles.filter(_.isFile).toList
      } else {
        List[File]()
      }
    }

    def getListOfFilesByExt(dir: File, extensions: List[String]): List[File] = {
      dir.listFiles.filter(_.isFile).toList.filter { file =>
        extensions.exists(file.getName.endsWith(_))
      }
    }

    val files = getListOfFilesByExt (new File("/data/ca"), List("txt"))

  //val news = files.map(a=>a.getAbsolutePath).map(file=>env.readTextFile(file).flatMap(line=>func(line).getTitle.toLowerCase.split("\\W+")).map { (_, 1) } .groupBy(0) .sum(1).print())
    val sources = files.map(a=>a.getAbsolutePath).map(file=>env.readTextFile(file)).reduceRight((a,b)=>a.union(b))
    val wordsInOrder = sources.flatMap(line=>func(line).getTitle.toLowerCase.split("\\W+")).filter{_.length > 0}.map { (_, 1) } .groupBy(0) .sum(1).sortPartition(1, Order.ASCENDING).print()

  }
}

