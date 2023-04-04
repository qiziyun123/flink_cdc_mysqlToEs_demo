package com.qizy.flinkcdc;

import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;

public class MysqlDemoSink extends RichSinkFunction<String> {

    /**
     * 监听后变化收到的信息
     * @param json
     * @param context
     */
    @Override
    public void invoke(String json, Context context) {
        System.out.println("===" +json);
    }

    @Override
    public void open(Configuration parameters) {
    }

    @Override
    public void close() {
    }
}
