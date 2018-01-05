package com.dag

import com.dag.source.RSSSources
import org.apache.flink.api.common.restartstrategy.RestartStrategies
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.api.scala._
import org.apache.flink.runtime.state.filesystem.FsStateBackend
import org.apache.flink.streaming.api.CheckpointingMode
import org.apache.flink.streaming.api.environment.CheckpointConfig.ExternalizedCheckpointCleanup
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment

object Job {
  def main(args: Array[String]) {

    val params = ParameterTool.fromArgs(args)
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.getConfig.setGlobalJobParameters(params)
    env.getConfig.disableSysoutLogging
if(false) {
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

    var newss = langs
      .filter(l => new DataService(pathData).getFeeds(l.name).size > 0)
      .map(l =>
        env.addSource(new RSSSources(l.name, 30, pathData)).uid(l.name + "-source")
          .map(a => (l.name, ("%04d%02d%02d" format(1900 + a.getDate.getYear, a.getDate.getMonth + 1, a.getDate.getDate)), a))
          .keyBy(0, 1).addSink(new FileSink).uid(l.name + "-sink")
      )

    env.execute("recover news");

  }
}

