package com.example.polls.service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.polls.model.KafkaOutbox;
import com.example.polls.repository.KafkaOutboxRepository;

@Service
@Transactional
public class KafkaProducerService {

    private static final Logger logger = LoggerFactory.getLogger(KafkaProducerService.class);

    private final KafkaTemplate<String, String> kafkaTemplate;

    private final KafkaOutboxRepository outboxRepository;

    private static final int MAX_RETRY_COUNT = 5;

    public KafkaProducerService(
            KafkaTemplate<String, String> kafkaTemplate,
            KafkaOutboxRepository outboxRepository) {
        this.kafkaTemplate = kafkaTemplate;
        this.outboxRepository = outboxRepository;
    }
    
    //在 sendWithPersistence 方法中使用事务，确保消息保存到数据库和发送到 Kafka 是一个原子操作。
    @Transactional
    public void sendWithPersistence(String topic, String key, String message) {
        // 先保存到数据库
        KafkaOutbox outbox = new KafkaOutbox(topic, key, message);
        outboxRepository.save(outbox);

        // 异步发送
        CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(topic, key, message);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                handleSuccess(outbox.getId());
            } else {
                handleFailure(outbox.getId(), ex);
            }
        });
    }

    private void handleSuccess(Long outboxId) {
        outboxRepository.findById(outboxId).ifPresent(outbox -> {
            outbox.setStatus("SENT");
            outbox.setSentAt(Instant.now());
            outboxRepository.save(outbox);
        });
    }

    private void handleFailure(Long outboxId, Throwable ex) {
        outboxRepository.findById(outboxId).ifPresent(outbox -> {
            if (outbox.getRetryCount() < MAX_RETRY_COUNT) {
                outbox.setStatus("FAILED");
                outbox.setRetryCount(outbox.getRetryCount() + 1);
                outboxRepository.save(outbox);
                // 记录异常日志
                logger.error("Failed to send message with outboxId: " + outboxId, ex);
            } else {
                // 达到最大重试次数，标记为死信或进行其他处理
                outbox.setStatus("DEAD_LETTER");
                outboxRepository.save(outbox);
                logger.error("Message with id {} reached maximum retry count, marked as dead letter", outboxId, ex);
            }
        });
    }

    @Scheduled(fixedDelay = 60000)
    public void retryFailedMessages() {
        List<KafkaOutbox> failedMessages = outboxRepository
                .findByStatusAndRetryCountLessThan("FAILED", MAX_RETRY_COUNT);
        // 重试机制优化：可以考虑使用指数退避算法来进行重试，避免频繁重试给系统带来过大压力。
        failedMessages.forEach(msg -> {
            int retryCount = msg.getRetryCount();
            long delay = (long) Math.pow(2, retryCount) * 1000; // 指数退避
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(msg.getTopic(),
                    msg.getMessageKey(), msg.getMessageBody());

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    handleSuccess(msg.getId());
                } else {
                    handleFailure(msg.getId(), ex);
                }
            });
        });
    }

}