package com.dag

import com.dag.news.bo.TempNew
import com.dag.source.RSSSources
import com.dag.utils.{TimeExtractorUtil, WindowUtil}
import org.apache.flink.api.common.restartstrategy.RestartStrategies
import org.apache.flink.api.java.tuple.Tuple
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.api.scala._
import org.apache.flink.runtime.state.filesystem.FsStateBackend
import org.apache.flink.streaming.api.CheckpointingMode
import org.apache.flink.streaming.api.environment.CheckpointConfig.ExternalizedCheckpointCleanup
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.api.windowing.windows.TimeWindow
import org.apache.flink.util.Collector
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.functions.timestamps.BoundedOutOfOrdernessTimestampExtractor

object Job {
  def main(args: Array[String]) {

    val params = ParameterTool.fromArgs(args)
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.getConfig.setGlobalJobParameters(params)
    //env.getConfig.disableSysoutLogging


    env.setStreamTimeCharacteristic(TimeCharacteristic.ProcessingTime)
    // alternatively:
    // env.setStreamTimeCharacteristic(TimeCharacteristic.IngestionTime);
    // env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);
    if (false) {
      // make parameters available in the web interface
      env.enableCheckpointing(1000L * 80 * 15)
      env.getCheckpointConfig.enableExternalizedCheckpoints(ExternalizedCheckpointCleanup.RETAIN_ON_CANCELLATION)
    }
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

    val pathData = params.get("pathDB", "tcp://localhost:9092/c:/work/scala/news/dbs/news")
    var langs = new DataService(pathData).getLanguages //.filter(p=>p.name == "en") // TODO develop
    /*
        val x = env.addSource(new RSSSources("en", 30, pathData)).uid("en" + "-source")
          .map(a => ("en", ("%04d%02d%02d" format(1900 + a.getDate.getYear, a.getDate.getMonth + 1, a.getDate.getDate)), a))
          .keyBy(0, 1)
          .window(TumblingEventTimeWindows.of(Time.minutes(60)))


        x.apply[(String, String, TempNew)]((key, window: TimeWindow, input: Iterable[(String, String, TempNew)], out: Collector[(String, String, TempNew)]) => {
          //input.foreach(a => out.collect(a))
        })
     var newss = langs
      .filter(l => new DataService(pathData).getFeeds(l.name).size > 0)
      .map(l =>
        env.addSource(new RSSSources(l.name, 30, pathData)).uid(l.name + "-source")
          //.assignAscendingTimestamps(a=>a.getDate.getTime)
          .assignTimestampsAndWatermarks(new TimeExtractorUtil(Time.days(1)))
          .map(a => (l.name, ("%04d%02d%02d" format(1900 + a.getDate.getYear, a.getDate.getMonth + 1, a.getDate.getDate)), a))
          .keyBy(0, 1)
          .window(TumblingEventTimeWindows.of(Time.minutes(1)))
          .apply[(String, String, TempNew)]((key: Tuple, window: TimeWindow, input: Iterable[(String, String, TempNew)], out: Collector[(String, String, TempNew)]) => { input.foreach(a => out.collect(a)) })
          //.apply[(String, String, TempNew)](new WindowUtil())
          //  .apply[(String, String, TempNew)](new WindowProcess())
          .addSink(new FileSink).uid(l.name + "-sink")
      )
*/
    var newss = langs
      .filter(l => new DataService(pathData).getFeeds(l.name).size > 0)
      .map(l =>
        env.addSource(new RSSSources(l.name, 30, pathData)).name(l.name + "-source")
          .map(a => (l.name, ("%04d%02d%02d" format(1900 + a.getDate.getYear, a.getDate.getMonth + 1, a.getDate.getDate)), a)).name(l.name + "-mapper")
          .keyBy(0, 1)
          .timeWindow(Time.minutes(60))
          .apply(new WindowProcess()).name(l.name + "-processor")
          .addSink(new FileSink).name(l.name + "-sink")
      )

    env.execute("recover news");

  }
}

