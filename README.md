# polling-app
Polling App built with Spring Boot, Spring Data JPA, Spring Security, PostgreSQL and JWT

install Kafka and Zookeeper (Windows)


cd C:\service\kafka_2.13-3.9.0

start Zookeeper and Kafka service:

bin\windows\zookeeper-server-start.bat config/zookeeper.properties
bin\windows\kafka-server-start.bat config/server.properties

create topics:

bin\windows\kafka-topics.bat --create --topic user-registered --bootstrap-server localhost:9092
bin\windows\kafka-topics.bat --create --topic poll-created --bootstrap-server localhost:9092
bin\windows\kafka-topics.bat --create --topic vote-casted --bootstrap-server localhost:9092

install Memurai (redis) on windows