package com.dag.utils;

import com.dag.news.bo.TempNew;
import org.apache.flink.api.java.tuple.Tuple;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.streaming.api.functions.windowing.WindowFunction;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;

public class WindowUtil implements WindowFunction<Tuple3<String, String, TempNew>, Tuple3<String, String, TempNew>, String, TimeWindow> {


    @Override
    public void apply(String s, TimeWindow window, Iterable<Tuple3<String, String, TempNew>> input, Collector<Tuple3<String, String, TempNew>> out) throws Exception {
        for (Tuple3<String, String, TempNew> item : input) {
            out.collect(item);
        }
    }
}
