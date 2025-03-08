package com.example.polls.service;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.polls.model.Poll;
import com.example.polls.model.User;
import com.example.polls.payload.ChoiceRequest;
import com.example.polls.payload.PollLength;
import com.example.polls.payload.PollRequest;
import com.example.polls.repository.PollRepository;
import com.example.polls.repository.UserRepository;
import com.example.polls.security.UserPrincipal;

@ExtendWith(MockitoExtension.class)
class PollServiceTest {

    @Mock
    private PollRepository pollRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserService userService;

    @Mock
    private KafkaProducerService kafkaProducerService;

    @InjectMocks
    private PollService pollService;

    @Test
    @DisplayName("创建投票 - 成功场景")
    void createPoll_Success() {
        // Given
        // 准备测试数据
        PollRequest pollRequest = new PollRequest();
        pollRequest.setQuestion("你最喜欢的编程语言是？");
        pollRequest.setChoices(Arrays.asList(
            new ChoiceRequest("Java"),
            new ChoiceRequest("Python")
        ));
        pollRequest.setPollLength(new PollLength(7, 0)); // 7天

        UserPrincipal currentUser = new UserPrincipal(1L, "", "", "user@example.com", "password", Collections.emptyList());
        User mockUser = new User();
        mockUser.setId(1L);
        
        Poll savedPoll = new Poll();
        savedPoll.setId(1L);
        savedPoll.setQuestion(pollRequest.getQuestion());

        // 配置 Mock 行为
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(pollRepository.save(any(Poll.class))).thenReturn(savedPoll);

        // When
        Poll result = pollService.createPoll(pollRequest, currentUser);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        
        // 验证依赖调用
        // 1. 验证用户积分添加
        verify(userService, times(1)).addPoints(mockUser, 10);
        
        // 2. 验证Kafka消息发送
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaProducerService).sendWithPersistence(
            eq("poll-created"),
            keyCaptor.capture(),
            messageCaptor.capture()
        );
        
        assertEquals("1", keyCaptor.getValue());
        assertTrue(messageCaptor.getValue().contains("\"question\":\"你最喜欢的编程语言是？\""));
        
        // 3. 验证投票属性
        ArgumentCaptor<Poll> pollCaptor = ArgumentCaptor.forClass(Poll.class);
        verify(pollRepository).save(pollCaptor.capture());
        
        Poll capturedPoll = pollCaptor.getValue();
        assertEquals(2, capturedPoll.getChoices().size());
        assertTrue(capturedPoll.getExpirationDateTime().isAfter(Instant.now()));
    }
}