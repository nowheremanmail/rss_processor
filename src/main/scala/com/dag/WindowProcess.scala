package com.dag

import com.dag.news.bo.TempNew
import org.apache.flink.api.common.state.MapStateDescriptor
import org.apache.flink.api.java.tuple.Tuple
import org.apache.flink.api.scala._
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.scala.function.ProcessWindowFunction
import org.apache.flink.streaming.api.windowing.windows.TimeWindow
import org.apache.flink.util.Collector

class WindowProcess extends ProcessWindowFunction[TempNew, TempNew, Tuple, TimeWindow] {
  val statusName = new MapStateDescriptor[String, Long]("starts", createTypeInformation[String], createTypeInformation[Long])

  override def open(parameters: Configuration): Unit = {
    //status = getRuntimeContext.getState(statusName)

  }

  override def clear(context: Context): Unit = {

  }

  override def process(key: Tuple, context: Context, input: Iterable[TempNew], out: Collector[TempNew]): Unit = {
    val already = context.windowState.getMapState(statusName)

    input.foreach(f => {
      if (!(already contains f.getLink)
      ) {
        out.collect(f)
        already.put(f.getLink, f.getDate.getTime)
      }
    }
    )
  }

}
