package com.dag

import com.dag.news.bo.TempNew
import org.apache.flink.api.java.tuple.Tuple
import org.apache.flink.streaming.api.scala.function.WindowFunction
import org.apache.flink.streaming.api.windowing.windows.TimeWindow
import org.apache.flink.util.Collector

class WindowProcess extends WindowFunction[(String, String, TempNew), (String, String, TempNew), Tuple, TimeWindow] {

  override def apply(key: Tuple, window: TimeWindow, input: Iterable[(String, String, TempNew)], out: Collector[(String, String, TempNew)]): Unit = {
    var already: Set[String] = Set()

    def key(a: (String, String, TempNew)) = a._3.getLink

    input.foreach(f => {
      if (!(already contains key(f))
      ) {
        out.collect(f)
        already += key(f)
      }
    }
    )
  }
}
