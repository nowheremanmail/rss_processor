package com.dag

import com.dag.news.bo.TempNew
import org.apache.flink.api.java.tuple.Tuple
import org.apache.flink.streaming.api.scala.function.WindowFunction
import org.apache.flink.streaming.api.windowing.windows.TimeWindow
import org.apache.flink.util.Collector

class WindowProcess extends WindowFunction[(String, String, TempNew), (String, String, TempNew), Tuple, TimeWindow] {
  def unique[A](ls: List[A]) = {
    def loop(set: Set[A], ls: List[A]): List[A] = ls match {
      case hd :: tail if set contains hd => loop(set, tail)
      case hd :: tail => hd :: loop(set + hd, tail)
      case Nil => Nil
    }

    loop(Set(), ls)
  }

  override def apply(key: Tuple, window: TimeWindow, input: Iterable[(String, String, TempNew)], out: Collector[(String, String, TempNew)]): Unit = {
    var already: Set[String] = Set()

    def key(a: TempNew) = a.getLink

    input.foreach(f => {
      if (!(already contains key(f._3))
      ) {
        out.collect(f)
        already += key(f._3)
      }
      else {

      }
    }
    )
  }
}
