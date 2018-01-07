package com.dag.utils;

import com.dag.news.bo.TempNew;
import org.apache.flink.streaming.api.functions.timestamps.BoundedOutOfOrdernessTimestampExtractor;

import org.apache.flink.streaming.api.windowing.time.Time;

public class TimeExtractorUtil extends BoundedOutOfOrdernessTimestampExtractor<TempNew> {

    public TimeExtractorUtil(Time maxOutOfOrderness) {
        super(maxOutOfOrderness);
    }

    @Override
    public long extractTimestamp(TempNew element) {
        //return element.getDate().getTime();
        return System.currentTimeMillis();
    }
}

