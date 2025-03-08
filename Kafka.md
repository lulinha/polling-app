启动 Kafka：

下载并解压 Kafka。

启动 Zookeeper 和 Kafka 服务：

cd C:\service\kafka_2.13-3.9.0

```bash
bin\windows\zookeeper-server-start.bat config/zookeeper.properties
bin\windows\kafka-server-start.bat config/server.properties
```

创建主题：
```bash
bin\windows\kafka-topics.bat --create --topic user-registered --bootstrap-server localhost:9092
bin\windows\kafka-topics.bat --create --topic poll-created --bootstrap-server localhost:9092
bin\windows\kafka-topics.bat --create --topic vote-casted --bootstrap-server localhost:9092
```

删除主题
```bash
bin\windows\kafka-topics.bat --delete  --topic user-registered --bootstrap-server localhost:9092
bin\windows\kafka-topics.bat --delete  --topic poll-created --bootstrap-server localhost:9092
bin\windows\kafka-topics.bat --delete  --topic vote-casted --bootstrap-server localhost:9092
```