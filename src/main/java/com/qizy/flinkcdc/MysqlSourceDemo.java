package com.qizy.flinkcdc;

import com.ververica.cdc.connectors.mysql.source.MySqlSource;
import com.ververica.cdc.connectors.mysql.table.StartupOptions;
import com.ververica.cdc.debezium.JsonDebeziumDeserializationSchema;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.configuration.RestOptions;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.configuration.Configuration;

import java.util.Properties;

public class MysqlSourceDemo {

    public static void main(String[] args) throws Exception {
        // 解决mysql decimal 类型解析后为字符串的问题 需要结合pom debezium的版本
        Properties debeziumProp = new Properties();
        debeziumProp.put("decimal.handling.mode", "string");

//        //config为属性文件名，放在包com.qizy.flinkcdc.config下，如果是放在src下，直接用mysql即可,属性文件不需要加.properties后缀名
//        ResourceBundle resource = ResourceBundle.getBundle("\\main\\java\\com\\qizy\\flinkcdc\\config");
//        String host = resource.getString("mysql.host");
//        int port = Integer.parseInt(resource.getString("mysql.port"));
//        String username = resource.getString("mysql.username");
//        String password = resource.getString("mysql.password");
        // todo 后续研究不使用springboot 项目如何读取配置文件
        MySqlSource<String> mySqlSource = MySqlSource.<String>builder()
                .hostname("1aaa")
                .port(3307)
//                .startupOptions(StartupOptions.initial()) //全量同步，貌似注释掉，扔会全量同步
                 // todo 通过时间戳来获取同步位置
                .startupOptions(StartupOptions.latest()) // 从binlog最新开始同步
                .scanNewlyAddedTableEnabled(true) // 开启支持新增表
                .databaseList("lbs_test") // set captured database
                .tableList("lbs_test.store_lbs_sync,lbs_test.store_lbs") // set captured table,数据库名必须写
                .username("aaa")
                .password("aaa")
                .serverTimeZone("Asia/Shanghai") // 这是东八区 mysql注意一定要对应设置 set time_zone='+8:00';
                .deserializer(new JsonDebeziumDeserializationSchema()) // converts SourceRecord to JSON String
                .includeSchemaChanges(true) // 表变更后得到通知
                .debeziumProperties(debeziumProp)
                .build();

        // 启动一个webUI
        Configuration configuration = new Configuration();
        configuration.setInteger(RestOptions.PORT,7001);
        // 第一次读取需要注释此行，后续增加表时，开启此行，
        // flink-ck后 ‘fd5f75bfb342fc101f9abd6e84482a92/chk-12404’换成存储路径下对应文件夹即可，实现旧表增量读取，新表全量读取
//        configuration.setString("execution.savepoint.path", "file:///tmp/flink-ck/fd5f75bfb342fc101f9abd6e84482a92/chk-12404");

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment(configuration);

        // enable checkpoint
        env.enableCheckpointing(5000);
        // 设置本地同步保存位置  todo 暂时也没明白为什么 会存在F 盘对应的tmp目录下，而不是项目启动环境下的
        env.getCheckpointConfig().setCheckpointStorage("file:///tmp/flink-ck");
        env.fromSource(mySqlSource, WatermarkStrategy.noWatermarks(), "MySQL Source")
//           .setParallelism(Runtime.getRuntime().availableProcessors()-2)
           .setParallelism(4)
           .addSink(new MysqlDemoSink());

        env.execute("Print MySQL Snapshot + Binlog");


    }
}
