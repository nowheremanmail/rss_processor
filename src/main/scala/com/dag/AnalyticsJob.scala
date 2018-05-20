package com.dag

import java.io.File

import com.dag.news.bo.TempNew
import org.apache.flink.api.common.operators.Order
import org.apache.flink.api.common.restartstrategy.RestartStrategies
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.api.scala._
import org.tartarus.snowball.SnowballStemmer
import org.tartarus.snowball.ext.SpanishStemmer

object AnalyticsJob {
  def main(args: Array[String]) {
    def func(json: String, stem: SnowballStemmer): Array[String] = {
      val title = JacksonWrapper.deserialize[TempNew](json).getTitle

      //title.toLowerCase.split("\\W+")
      title.toLowerCase.split("(\\p{Z}|[;:\\|\\\\/])+").map(a => {
        var tt = a

        while (tt.length() >= 1 && tt.substring(0, 1).matches("[\\p{P}&&[^#@]]|«|»|“|”|‘|’|´|\\?|¿|!|¡")) {
          tt = tt.substring(1)
        }

        while (tt.length() > 1
          && tt.substring(tt.length() - 1, tt.length()).matches("\\p{P}|«|»|“|”|‘|’|´|\\?|¿|!|¡")) {
          tt = tt.substring(0, tt.length() - 1)
        }

        /*if (tt.length() > 0) {
          if (isNumber(tt, language)) {
            // TODO if is number return semantic number???
            continue;
          }

          if (stopWords.contains(tt))
            continue;

        }*/
        if (stem != null) {
          stem.setCurrent(tt);
          if (stem.stem())
            tt = stem.getCurrent();
        }

        tt
      })
    }

    val params = ParameterTool.fromArgs(args)
    val env = ExecutionEnvironment.getExecutionEnvironment

    env.getConfig.setGlobalJobParameters(params)

    env.setParallelism(1)
    env.setRestartStrategy(RestartStrategies.noRestart())
    env.getConfig.disableSysoutLogging

    def getListOfFiles(dir: String): List[File] = {
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

    val files = getListOfFilesByExt(new File("/data/es"), List("txt"))

    val sources = files.map(a => a.getAbsolutePath).map(file => env.readTextFile(file)).reduceRight((a, b) => a.union(b))
    val wordsInOrder = sources.flatMap(line => func(line, new SpanishStemmer())).filter {
      _.length > 0
    }.map {
      (_, 1)
    }.groupBy(0).sum(1).sortPartition(1, Order.ASCENDING).print()

  }
}

