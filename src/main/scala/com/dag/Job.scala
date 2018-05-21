package com.dag

import com.dag.news.bo.TempNew
import com.dag.source.RSSSources
import org.apache.flink.api.common.restartstrategy.RestartStrategies
import org.apache.flink.api.common.restartstrategy.RestartStrategies.RestartStrategyConfiguration
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.api.scala._
import org.apache.flink.runtime.executiongraph.restart.RestartStrategy
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.environment.CheckpointConfig.ExternalizedCheckpointCleanup
import org.apache.flink.streaming.api.functions.AssignerWithPunctuatedWatermarks
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.streaming.api.watermark.Watermark
import org.apache.flink.streaming.api.windowing.time.Time
import org.slf4j.LoggerFactory

object Job {
  val LOG = LoggerFactory.getLogger("Job");

  def main(args: Array[String]) {

    val params = ParameterTool.fromArgs(args)
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.getConfig.setGlobalJobParameters(params)
    //env.getConfig.disableSysoutLogging

    env.getConfig.setRestartStrategy(RestartStrategies.fixedDelayRestart(10000, 60000))

    //env.setStreamTimeCharacteristic(TimeCharacteristic.ProcessingTime)
    // alternatively:
    // env.setStreamTimeCharacteristic(TimeCharacteristic.IngestionTime);
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);

      env.enableCheckpointing(1000L * 60 * 15)
      env.getCheckpointConfig.enableExternalizedCheckpoints(ExternalizedCheckpointCleanup.RETAIN_ON_CANCELLATION)

    /*
        //env.setStateBackend(new FsStateBackend("file://c:/tmp/checkpoints"))
        // advanced options:
        // set mode to exactly-once (this is the default)
        env.getCheckpointConfig.setCheckpointingMode(CheckpointingMode.EXACTLY_ONCE)

        // make sure 500 ms of progress happen between checkpoints
        env.getCheckpointConfig.setMinPauseBetweenCheckpoints(5000)

        // checkpoints have to complete within one minute, or are discarded
        env.getCheckpointConfig.setCheckpointTimeout(60000)

        // allow only one checkpoint to be in progress at the same time
        env.getCheckpointConfig.setMaxConcurrentCheckpoints(1)
    */

    val bingKey = params.get("bingKey")

    val pathData = params.get("pathDB", "./dbs/news")

    val ds = new DataService(pathData)

    var sources = ds.getLanguages
      .filter(l => ds.getFeeds(l.name).size > 0)
      //.flatMap(l => ds.getFeeds(l.name))
      .map(f => env
      .addSource(new RSSSources(f.name, 30, pathData, bingKey)).uid("source-" + f.name)
    ).reduce((a, b) => a.union(b))

    sources.filter(f => f.getDate != null).uid("filter") //uid("filter-" + f.name)
      .assignTimestampsAndWatermarks(new AssignerWithPunctuatedWatermarks[TempNew](){
      override def checkAndGetNextWatermark(lastElement: TempNew, extractedTimestamp: Long): Watermark = new Watermark(extractedTimestamp)

      override def extractTimestamp(element: TempNew, previousElementTimestamp: Long): Long = element.getDate.getTime
    }).uid("watermark")//.uid("watermark-" + f.name)
      .keyBy("language", "day")
      .timeWindow(Time.hours(1))
      .allowedLateness(Time.hours(1))
      .process(new WindowProcess()).uid("process")//.uid("process-" + f.name)
      .addSink(new FileSink).uid("sink")//.uid("sink-" + f.name)


    env.execute("recover news");

  }
}

