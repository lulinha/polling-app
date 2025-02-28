package com.example.polls.service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.polls.model.Choice;
import com.example.polls.model.Poll;
import com.example.polls.payload.ChoiceRequest;
import com.example.polls.payload.PollLength;
import com.example.polls.payload.PollRequest;
import com.example.polls.repository.PollRepository;
import com.example.polls.repository.VoteRepository;

//  JUnit 5 中用于启用 Mockito 的注解
@ExtendWith(MockitoExtension.class)
public class PollServiceTest {

    // 创建模拟对象
    @Mock
    private PollRepository pollRepository;

    @Mock
    private VoteRepository voteRepository;

    @Mock
    private KafkaProducerService kafkaProducerService;

    // 将模拟对象注入到 PollService 中
    @InjectMocks
    private PollService pollService;

    private PollRequest validRequest;
    private Poll savedPoll;

    // 该方法会在每个测试方法执行之前运行，用于初始化测试所需的数据
    @BeforeEach
    void setUp() {
        // 初始化测试数据
        validRequest = new PollRequest();
        validRequest.setQuestion("Question A");

        List<ChoiceRequest> choices = Stream.of("Choice A", "Choice B", "Choice C", "Choice D")
                .map(text -> {
                    ChoiceRequest choiceRequest = new ChoiceRequest();
                    choiceRequest.setText(text);
                    return choiceRequest;
                })
                .collect(Collectors.toList());
        validRequest.setChoices(choices);

        PollLength pollLength = new PollLength();
        pollLength.setDays(1);
        pollLength.setHours(0);
        validRequest.setPollLength(pollLength);

        // 模拟保存后的 Poll 对象（带 ID）
        savedPoll = new Poll();
        savedPoll.setId(1L);
        savedPoll.setQuestion(validRequest.getQuestion());
        validRequest.getChoices().forEach(choiceRequest -> {
            savedPoll.addChoice(new Choice(choiceRequest.getText()));
        });
    }

    @Test
    void createPoll_WithValidRequest_ShouldReturnSavedPoll() {

        // 1. 配置 Mock 行为. 
        when(pollRepository.save(any(Poll.class))).thenReturn(savedPoll);

        // 2. 调用方法
        Poll result = pollService.createPoll(validRequest);

        // 3. 验证结果
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(validRequest.getQuestion(), result.getQuestion());

        // 4. 验证 Mock 交互
        verify(pollRepository, times(1)).save(any(Poll.class));
    }

}
