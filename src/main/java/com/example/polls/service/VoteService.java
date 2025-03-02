package com.example.polls.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.example.polls.dto.VoteDTO;
import com.example.polls.exception.BadRequestException;
import com.example.polls.exception.ResourceNotFoundException;
import com.example.polls.model.Choice;
import com.example.polls.model.ChoiceVoteCount;
import com.example.polls.model.Poll;
import com.example.polls.model.User;
import com.example.polls.model.Vote;
import com.example.polls.payload.PollResponse;
import com.example.polls.payload.VoteRequest;
import com.example.polls.repository.PollRepository;
import com.example.polls.repository.UserRepository;
import com.example.polls.repository.VoteRepository;
import com.example.polls.security.UserPrincipal;
import com.example.polls.util.JsonUtils;
import com.example.polls.util.ModelMapper;

@Service
public class VoteService {

    @Autowired
    private PollRepository pollRepository;

    @Autowired
    private VoteRepository voteRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private KafkaProducerService kafkaProducerService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String VOTE_COUNT_KEY = "poll_votes:";

    private static final Logger logger = LoggerFactory.getLogger(VoteService.class);

    public PollResponse castVoteAndGetUpdatedPoll(Long pollId, VoteRequest voteRequest, UserPrincipal currentUser) {
        Poll poll = pollRepository.findById(pollId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Poll with id" + pollId + " not found"));

        if (poll.getExpirationDateTime().isBefore(Instant.now())) {
            throw new BadRequestException("Sorry! This Poll has already expired");
        }

        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User with id " + currentUser.getId() + " not found"));

        Choice selectedChoice = poll.getChoices().stream()
                .filter(choice -> choice.getId().equals(voteRequest.getChoiceId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Choice with id" + voteRequest.getChoiceId() + " not found"));

        Vote vote = new Vote();
        vote.setPoll(poll);
        vote.setUser(user);
        vote.setChoice(selectedChoice);

        try {
            vote = voteRepository.save(vote);
            // 将投票信息转换为 DTO
            VoteDTO voteDTO = new VoteDTO(vote.getId(), poll.getId(), selectedChoice.getId(), user.getId());

            // 发送投票提交事件
            // kafkaProducerService.sendVoteSubmittedEvent(vote.getId());
            // 将 DTO 转换为 JSON
            String message = JsonUtils.toJson(voteDTO);
            kafkaProducerService.sendWithPersistence("vote-casted", vote.getId().toString(), message);
        } catch (DataIntegrityViolationException ex) {
            logger.info("User {} has already voted in Poll {}", currentUser.getId(), pollId);
            throw new BadRequestException("Sorry! You have already cast your vote in this poll");
        }

        // -- Vote Saved, Return the updated Poll Response now --

        // Retrieve Vote Counts of every choice belonging to the current poll
        List<ChoiceVoteCount> votes = voteRepository.countByPollIdGroupByChoiceId(pollId);

        Map<Long, Long> choiceVotesMap = votes.stream()
                .collect(Collectors.toMap(ChoiceVoteCount::getChoiceId, ChoiceVoteCount::getVoteCount));

        // Retrieve poll creator details
        User creator = userRepository.findById(poll.getCreatedBy())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User with id" + poll.getCreatedBy() + " not found"));

        return ModelMapper.mapPollToPollResponse(poll, choiceVotesMap, creator, vote.getChoice().getId());
    }

    public PollResponse castVoteAndGetUpdatedPollWithCache(Long pollId, VoteRequest voteRequest, UserPrincipal currentUser) {
        Poll poll = pollRepository.findById(pollId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Poll with id" + pollId + " not found"));

        if (poll.getExpirationDateTime().isBefore(Instant.now())) {
            throw new BadRequestException("Sorry! This Poll has already expired");
        }

        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User with id " + currentUser.getId() + " not found"));

        Choice selectedChoice = poll.getChoices().stream()
                .filter(choice -> choice.getId().equals(voteRequest.getChoiceId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Choice with id" + voteRequest.getChoiceId() + " not found"));

        Vote vote = new Vote();
        vote.setPoll(poll);
        vote.setUser(user);
        vote.setChoice(selectedChoice);

        try {
            vote = voteRepository.save(vote);
            Long choiceId = selectedChoice.getId();

            // 更新Redis统计
            String voteCountKey = VOTE_COUNT_KEY + pollId;
            redisTemplate.opsForHash().increment(voteCountKey, choiceId.toString(), 1);

            // 将投票信息转换为 DTO
            VoteDTO voteDTO = new VoteDTO(vote.getId(), poll.getId(), selectedChoice.getId(), user.getId());

            // 发送投票提交事件
            // kafkaProducerService.sendVoteSubmittedEvent(vote.getId());
            // 将 DTO 转换为 JSON
            String message = JsonUtils.toJson(voteDTO);
            kafkaProducerService.sendWithPersistence("vote-casted", vote.getId().toString(), message);
        } catch (DataIntegrityViolationException ex) {
            logger.info("User {} has already voted in Poll {}", currentUser.getId(), pollId);
            throw new BadRequestException("Sorry! You have already cast your vote in this poll");
        }

        // -- Vote Saved, Return the updated Poll Response now --

        // Retrieve Vote Counts of every choice belonging to the current poll
        List<ChoiceVoteCount> votes = voteRepository.countByPollIdGroupByChoiceId(pollId);

        Map<Long, Long> choiceVotesMap = votes.stream()
                .collect(Collectors.toMap(ChoiceVoteCount::getChoiceId, ChoiceVoteCount::getVoteCount));

        // Retrieve poll creator details
        User creator = userRepository.findById(poll.getCreatedBy())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User with id" + poll.getCreatedBy() + " not found"));
        // 缓存用户投票记录
        String userVoteKey = "user_votes:" + currentUser.getId();
        redisTemplate.opsForHash().put(userVoteKey, pollId.toString(), choiceId);

        return ModelMapper.mapPollToPollResponse(poll, choiceVotesMap, creator, vote.getChoice().getId());
    }

    /**
     * 获取指定投票的统计信息，返回每个选项的投票数
     * 
     * @param pollId 投票的 ID
     * @return 包含选项 ID 和对应投票数的映射
     */
    public Map<Long, Long> getVoteStatistics(Long pollId) {
        // 从数据库中获取指定 pollId 下每个选项的投票数
        List<ChoiceVoteCount> votes = voteRepository.countByPollIdGroupByChoiceId(pollId);

        // 将查询结果转换为 Map，键为选项 ID，值为投票数
        Map<Long, Long> voteStatistics = votes.stream()
                .collect(Collectors.toMap(ChoiceVoteCount::getChoiceId, ChoiceVoteCount::getVoteCount));

        return voteStatistics;
    }

    public List<Vote> getVotesByUser(Long userId) {
        // 从数据库中获取指定用户的所有投票记录
        List<Vote> userVotes = voteRepository.findByUserId(userId);

        return userVotes;
    }

}
