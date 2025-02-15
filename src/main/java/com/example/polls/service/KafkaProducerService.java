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

    public KafkaProducerService(
            KafkaTemplate<String, String> kafkaTemplate,
            KafkaOutboxRepository outboxRepository) {
        this.kafkaTemplate = kafkaTemplate;
        this.outboxRepository = outboxRepository;
    }

    public void sendWithPersistence(String topic, String key, String message) {
        // 先保存到数据库
        KafkaOutbox outbox = new KafkaOutbox();
        outbox.setTopic(topic);
        outbox.setMessageKey(key);
        outbox.setMessageBody(message);
        outbox.setStatus("PENDING");
        outbox.setCreatedAt(Instant.now());
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
            outbox.setStatus("FAILED");
            outbox.setRetryCount(outbox.getRetryCount() + 1);
            outboxRepository.save(outbox);
        });
    }

    @Scheduled(fixedDelay = 60000)
    public void retryFailedMessages() {
        List<KafkaOutbox> failedMessages = outboxRepository
                .findByStatusAndRetryCountLessThan("FAILED", 5);

        failedMessages.forEach(msg -> {
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