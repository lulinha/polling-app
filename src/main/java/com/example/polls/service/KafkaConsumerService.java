package com.example.polls.service;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.example.polls.dto.PollDTO;
import com.example.polls.dto.UserDTO;
import com.example.polls.dto.VoteDTO;
import com.example.polls.model.KafkaDeadLetter;
import com.example.polls.repository.KafkaDeadLetterRepository;
import com.example.polls.util.JsonUtils;

@Service
public class KafkaConsumerService {

    private static final Logger logger = LoggerFactory.getLogger(KafkaConsumerService.class);

    private final KafkaDeadLetterRepository deadLetterRepository;
    private final NotificationService notificationService;

    public KafkaConsumerService(KafkaDeadLetterRepository deadLetterRepository,
            NotificationService notificationService) {
        this.deadLetterRepository = deadLetterRepository;
        this.notificationService = notificationService;
    }

    @KafkaListener(topics = "poll-created", groupId = "polling-app-group")
    public void handlePollCreated(String message) {
        try {
            PollDTO pollDTO = JsonUtils.fromJson(message, PollDTO.class);
            notificationService.notifyAdmins(pollDTO);
            logger.info("Processed poll creation: {}", pollDTO.getId());
        } catch (Exception ex) {
            logger.error("Failed to process poll creation", ex);
            // 将无法处理的消息发送到死信队列，以便后续处理。
            saveToDeadLetter("poll-created", null, message, ex);
            throw new RuntimeException(ex);
        }
    }

    private void saveToDeadLetter(String topic, String key, String message, Exception ex) {
        KafkaDeadLetter deadLetter = new KafkaDeadLetter();
        deadLetter.setTopic(topic);
        deadLetter.setMessageKey(key);
        deadLetter.setMessageBody(message);
        deadLetter.setError(ex.getMessage());
        deadLetter.setCreatedAt(Instant.now());
        deadLetterRepository.save(deadLetter);
    }

    @KafkaListener(topics = "user-registered", groupId = "user-management-group")
    public void handleUserRegistered(String message) {
        try {
            // 将 JSON 消息反序列化为 UserDTO
            UserDTO userDTO = JsonUtils.fromJson(message, UserDTO.class);

            // 处理用户注册逻辑，例如发送欢迎邮件、更新缓存等
            processUserRegistration(userDTO);
        } catch (Exception ex) {
            // 将无法处理的消息发送到死信队列，以便后续处理。
            saveToDeadLetter("user-registered", null, message, ex);
            logger.error("Failed to process user registration message: " + message, ex);
        }
    }

    private void processUserRegistration(UserDTO userDTO) {
        // 例如发送欢迎邮件
        sendWelcomeEmail(userDTO.getEmail());

        // 例如更新缓存
        updateUserCache(userDTO);
    }

    private void sendWelcomeEmail(String email) {
        // 实现发送邮件的逻辑
    }

    private void updateUserCache(UserDTO userDTO) {
        // 实现更新缓存的逻辑
    }

    @KafkaListener(topics = "vote-casted", groupId = "vote-processing-group")
    public void handleVoteCasted(String message) {
        // 处理投票提交事件，例如更新统计信息
        try {
            // 将 JSON 消息反序列化为 VoteDTO
            VoteDTO voteDTO = JsonUtils.fromJson(message, VoteDTO.class);

            // 处理投票逻辑，例如更新投票统计、发送通知等
            processVote(voteDTO);
        } catch (Exception ex) {
            saveToDeadLetter("vote-casted", null, message, ex);
            logger.error("Failed to process vote casted message: " + message, ex);
        }
    }

    private void processVote(VoteDTO voteDTO) {
        // 例如更新投票统计
        updateVoteStatistics(voteDTO.getPollId(), voteDTO.getChoiceId());

        // 例如发送通知
        sendVoteNotification(voteDTO.getUserId(), voteDTO.getPollId());
    }

    private void updateVoteStatistics(Long pollId, Long choiceId) {
        // 实现更新投票统计的逻辑
    }

    private void sendVoteNotification(Long userId, Long pollId) {
        // 实现发送通知的逻辑
    }
}