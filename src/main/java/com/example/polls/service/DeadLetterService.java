package com.example.polls.service;

import java.util.List;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.example.polls.model.KafkaDeadLetter;
import com.example.polls.repository.KafkaDeadLetterRepository;

@Service
public class DeadLetterService {

    private final KafkaDeadLetterRepository deadLetterRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public DeadLetterService(KafkaDeadLetterRepository deadLetterRepository, KafkaTemplate<String, String> kafkaTemplate) {
        this.deadLetterRepository = deadLetterRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelay = 300000) // 每5分钟处理一次
    public void reprocessDeadLetters() {
        List<KafkaDeadLetter> letters = deadLetterRepository.findByRetryCountLessThan(3);
        
        letters.forEach(letter -> {
            try {
                kafkaTemplate.send(letter.getTopic(), letter.getMessageKey(), letter.getMessageBody());
                deadLetterRepository.delete(letter);
            } catch (Exception ex) {
                letter.setRetryCount(letter.getRetryCount() + 1);
                deadLetterRepository.save(letter);
            }
        });
    }
}