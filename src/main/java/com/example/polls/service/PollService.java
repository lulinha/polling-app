package com.example.polls.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.polls.dto.PollDTO;
import com.example.polls.exception.BadRequestException;
import com.example.polls.exception.ResourceNotFoundException;
import com.example.polls.model.Choice;
import com.example.polls.model.ChoiceVoteCount;
import com.example.polls.model.Poll;
import com.example.polls.model.User;
import com.example.polls.model.Vote;
import com.example.polls.payload.PagedResponse;
import com.example.polls.payload.PollRequest;
import com.example.polls.payload.PollResponse;
import com.example.polls.repository.PollRepository;
import com.example.polls.repository.UserRepository;
import com.example.polls.repository.VoteRepository;
import com.example.polls.security.UserPrincipal;
import com.example.polls.util.AppConstants;
import com.example.polls.util.JsonUtils;
import com.example.polls.util.ModelMapper;

@Service
public class PollService {

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
        private static final String POLL_CREATOR_KEY = "poll_creators:";

        private static final Logger logger = LoggerFactory.getLogger(PollService.class);

        public PagedResponse<PollResponse> getAllPolls(UserPrincipal currentUser, int page, int size) {
                validatePageNumberAndSize(page, size);

                // Retrieve Polls
                Pageable pageable = PageRequest.of(page, size, Sort.Direction.DESC, "createdAt");
                Page<Poll> polls = pollRepository.findAll(pageable);

                if (polls.getNumberOfElements() == 0) {
                        return new PagedResponse<>(Collections.emptyList(), page, size, 0, 0, true);
                }

                // Map Polls to PollResponses containing vote counts and poll creator details
                List<Long> pollIds = polls.map(Poll::getId).getContent();
                Map<Long, Long> choiceVoteCountMap = getChoiceVoteCountMap(pollIds);
                Map<Long, Long> pollUserVoteMap = getPollUserVoteMap(currentUser, pollIds);
                Map<Long, User> creatorMap = getPollCreatorMap(polls.getContent());

                List<PollResponse> pollResponses = polls.map(poll -> {
                        return ModelMapper.mapPollToPollResponse(poll,
                                        choiceVoteCountMap,
                                        creatorMap.get(poll.getCreatedBy()),
                                        pollUserVoteMap == null ? null
                                                        : pollUserVoteMap.getOrDefault(poll.getId(), null));
                }).getContent();

                return new PagedResponse<>(pollResponses, polls.getNumber(),
                                polls.getSize(), polls.getTotalElements(), polls.getTotalPages(), polls.isLast());
        }

        public PagedResponse<PollResponse> getAllPollsWithCache(UserPrincipal currentUser, int page, int size) {
                validatePageNumberAndSize(page, size);

                // 使用Redis缓存分页元数据
                String pageKey = "polls_page:" + page + "_" + size;
                PagedResponse<PollResponse> cachedResponse = (PagedResponse<PollResponse>) redisTemplate.opsForValue()
                                .get(pageKey);

                if (cachedResponse != null) {
                        logger.debug("Cache hit for page key: {}", pageKey);
                        return cachedResponse;
                }

                // Retrieve Polls
                Pageable pageable = PageRequest.of(page, size, Sort.Direction.DESC, "createdAt");
                Page<Poll> polls = pollRepository.findAll(pageable);

                if (polls.getNumberOfElements() == 0) {
                        return new PagedResponse<>(Collections.emptyList(), page, size, 0, 0, true);
                }

                // Map Polls to PollResponses containing vote counts and poll creator details
                List<Long> pollIds = polls.map(Poll::getId).getContent();
                Map<Long, Long> choiceVoteCountMap = getChoiceVoteCountMap(pollIds);
                Map<Long, Long> pollUserVoteMap = getPollUserVoteMap(currentUser, pollIds);
                Map<Long, User> creatorMap = getPollCreatorMap(polls.getContent());

                List<PollResponse> pollResponses = polls.map(poll -> {
                        return ModelMapper.mapPollToPollResponse(poll,
                                        choiceVoteCountMap,
                                        creatorMap.get(poll.getCreatedBy()),
                                        pollUserVoteMap == null ? null
                                                        : pollUserVoteMap.getOrDefault(poll.getId(), null));
                }).getContent();

                PagedResponse<PollResponse> result = new PagedResponse<>(pollResponses, polls.getNumber(),
                                polls.getSize(), polls.getTotalElements(), polls.getTotalPages(), polls.isLast());

                // 缓存结果（添加随机偏移防止雪崩,避免同一时间大量缓存同时过期导致的数据库压力）
                Random rand = new Random();
                int offset = rand.nextInt(60); // 0-59秒随机偏移
                redisTemplate.opsForValue().set(
                                pageKey,
                                result,
                                Duration.ofMinutes(5).plusSeconds(offset));

                return result;
        }

        public PagedResponse<PollResponse> getPollsCreatedBy(String username, UserPrincipal currentUser, int page,
                        int size) {
                validatePageNumberAndSize(page, size);

                User user = userRepository.findByUsername(username)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "User with username" + username + " not found"));

                // Retrieve all polls created by the given username
                Pageable pageable = PageRequest.of(page, size, Sort.Direction.DESC, "createdAt");
                Page<Poll> polls = pollRepository.findByCreatedBy(user.getId(), pageable);

                if (polls.getNumberOfElements() == 0) {
                        return new PagedResponse<>(Collections.emptyList(), polls.getNumber(),
                                        polls.getSize(), polls.getTotalElements(), polls.getTotalPages(),
                                        polls.isLast());
                }

                // Map Polls to PollResponses containing vote counts and poll creator details
                List<Long> pollIds = polls.map(Poll::getId).getContent();
                Map<Long, Long> choiceVoteCountMap = getChoiceVoteCountMap(pollIds);
                Map<Long, Long> pollUserVoteMap = getPollUserVoteMap(currentUser, pollIds);

                List<PollResponse> pollResponses = polls.map(poll -> {
                        return ModelMapper.mapPollToPollResponse(poll,
                                        choiceVoteCountMap,
                                        user,
                                        pollUserVoteMap == null ? null
                                                        : pollUserVoteMap.getOrDefault(poll.getId(), null));
                }).getContent();

                return new PagedResponse<>(pollResponses, polls.getNumber(),
                                polls.getSize(), polls.getTotalElements(), polls.getTotalPages(), polls.isLast());
        }

        public PagedResponse<PollResponse> getPollsVotedBy(String username, UserPrincipal currentUser, int page,
                        int size) {
                validatePageNumberAndSize(page, size);

                User user = userRepository.findByUsername(username)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "User with username" + username + " not found"));

                // Retrieve all pollIds in which the given username has voted
                Pageable pageable = PageRequest.of(page, size, Sort.Direction.DESC, "createdAt");
                Page<Long> userVotedPollIds = voteRepository.findVotedPollIdsByUserId(user.getId(), pageable);

                if (userVotedPollIds.getNumberOfElements() == 0) {
                        return new PagedResponse<>(Collections.emptyList(), userVotedPollIds.getNumber(),
                                        userVotedPollIds.getSize(), userVotedPollIds.getTotalElements(),
                                        userVotedPollIds.getTotalPages(), userVotedPollIds.isLast());
                }

                // Retrieve all poll details from the voted pollIds.
                List<Long> pollIds = userVotedPollIds.getContent();

                Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
                List<Poll> polls = pollRepository.findByIdIn(pollIds, sort);

                // Map Polls to PollResponses containing vote counts and poll creator details
                Map<Long, Long> choiceVoteCountMap = getChoiceVoteCountMap(pollIds);
                Map<Long, Long> pollUserVoteMap = getPollUserVoteMap(currentUser, pollIds);
                Map<Long, User> creatorMap = getPollCreatorMap(polls);

                List<PollResponse> pollResponses = polls.stream().map(poll -> {
                        return ModelMapper.mapPollToPollResponse(poll,
                                        choiceVoteCountMap,
                                        creatorMap.get(poll.getCreatedBy()),
                                        pollUserVoteMap == null ? null
                                                        : pollUserVoteMap.getOrDefault(poll.getId(), null));
                }).collect(Collectors.toList());

                return new PagedResponse<>(pollResponses, userVotedPollIds.getNumber(), userVotedPollIds.getSize(),
                                userVotedPollIds.getTotalElements(), userVotedPollIds.getTotalPages(),
                                userVotedPollIds.isLast());
        }

        @Transactional
        public Poll createPoll(PollRequest pollRequest) {
                Poll poll = new Poll();
                poll.setQuestion(pollRequest.getQuestion());

                pollRequest.getChoices().forEach(choiceRequest -> {
                        poll.addChoice(new Choice(choiceRequest.getText()));
                });

                Instant now = Instant.now();
                Instant expirationDateTime = now.plus(Duration.ofDays(pollRequest.getPollLength().getDays()))
                                .plus(Duration.ofHours(pollRequest.getPollLength().getHours()));

                poll.setExpirationDateTime(expirationDateTime);

                // 保存投票
                Poll savedPoll = pollRepository.save(poll);

                // 使用持久化发送
                PollDTO pollDTO = ModelMapper.convertToPollDTO(savedPoll);
                String message = JsonUtils.toJson(pollDTO);
                logger.info("Sending message: {}", message);
                kafkaProducerService.sendWithPersistence("poll-created", savedPoll.getId().toString(), message);

                return savedPoll;
        }

        @Transactional
        @CacheEvict(allEntries = true) // 创建新投票时清除所有缓存（根据业务需求调整）
        public Poll createPollWithCache(PollRequest pollRequest) {
                Poll poll = new Poll();
                poll.setQuestion(pollRequest.getQuestion());

                pollRequest.getChoices().forEach(choiceRequest -> {
                        poll.addChoice(new Choice(choiceRequest.getText()));
                });

                Instant now = Instant.now();
                Instant expirationDateTime = now.plus(Duration.ofDays(pollRequest.getPollLength().getDays()))
                                .plus(Duration.ofHours(pollRequest.getPollLength().getHours()));

                poll.setExpirationDateTime(expirationDateTime);

                // 保存投票
                Poll savedPoll = pollRepository.save(poll);
                // 预热投票统计缓存
                initializeVoteCountsInRedis(savedPoll);

                // 使用持久化发送
                PollDTO pollDTO = ModelMapper.convertToPollDTO(savedPoll);
                String message = JsonUtils.toJson(pollDTO);
                logger.info("Sending message: {}", message);
                kafkaProducerService.sendWithPersistence("poll-created", savedPoll.getId().toString(), message);

                return savedPoll;
        }

        private void initializeVoteCountsInRedis(Poll poll) {
                String key = VOTE_COUNT_KEY + poll.getId();
                Map<String, Integer> initialCounts = poll.getChoices().stream()
                                .collect(Collectors.toMap(
                                                choice -> choice.getId().toString(),
                                                choice -> 0));
                redisTemplate.opsForHash().putAll(key, initialCounts);
                redisTemplate.expire(key, Duration.ofDays(30)); // 设置合理过期时间
        }

        @Cacheable(key = "#pollId", unless = "#result == null")
        public PollResponse getPollByIdWithCache(Long pollId, UserPrincipal currentUser) {
                Poll poll = pollRepository.findById(pollId).orElseThrow(
                                () -> new ResourceNotFoundException("Poll with id " + pollId + " not found"));

                // 使用Redis Hash获取实时投票统计
                Map<Long, Long> choiceVotesMap = getChoiceVoteCountMapFromRedis(pollId);

                // 获取创建者信息（带缓存）
                User creator = getCachedUser(poll.getCreatedBy());

                // 获取用户投票记录
                Long userVoteChoiceId = getCachedUserVote(currentUser, pollId);

                return ModelMapper.mapPollToPollResponse(
                                poll,
                                choiceVotesMap,
                                creator,
                                userVoteChoiceId);
        }

        private Map<Long, Long> getChoiceVoteCountMapFromRedis(Long pollId) {
                String key = VOTE_COUNT_KEY + pollId;
                Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
                return entries.entrySet().stream()
                                .collect(Collectors.toMap(
                                                e -> Long.parseLong(e.getKey().toString()),
                                                e -> Long.parseLong(e.getValue().toString())));
        }

        @Cacheable(value = "users", key = "#userId")
        public User getCachedUser(Long userId) {
                return userRepository.findById(userId)
                                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        }

        private Long getCachedUserVote(UserPrincipal currentUser, Long pollId) {
                if (currentUser == null)
                        return null;

                String userVoteKey = "user_votes:" + currentUser.getId();
                Object vote = redisTemplate.opsForHash().get(userVoteKey, pollId.toString());
                return vote != null ? Long.parseLong(vote.toString()) : null;
        }

        public PollResponse getPollById(Long pollId, UserPrincipal currentUser) {
                Poll poll = pollRepository.findById(pollId).orElseThrow(
                                () -> new ResourceNotFoundException("Poll with id " + pollId + " not found"));

                // Retrieve Vote Counts of every choice belonging to the current poll
                List<ChoiceVoteCount> votes = voteRepository.countByPollIdGroupByChoiceId(pollId);

                Map<Long, Long> choiceVotesMap = votes.stream()
                                .collect(Collectors.toMap(ChoiceVoteCount::getChoiceId, ChoiceVoteCount::getVoteCount));

                // Retrieve poll creator details
                User creator = userRepository.findById(poll.getCreatedBy())
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "User with id" + poll.getCreatedBy() + " not found"));

                // Retrieve vote done by logged in user
                Vote userVote = null;
                if (currentUser != null) {
                        userVote = voteRepository.findByUserIdAndPollId(currentUser.getId(), pollId);
                }

                return ModelMapper.mapPollToPollResponse(poll, choiceVotesMap,
                                creator, userVote != null ? userVote.getChoice().getId() : null);
        }

        public Poll updatePoll(Long pollId, PollRequest pollRequest) {
                // 更新投票逻辑
                return null;
        }

        public void deletePoll(Long pollId) {
                // 删除投票逻辑
        }

        @CacheEvict(key = "#pollId")
        public void deletePollWithCache(Long pollId) {
                // 删除数据库记录
                pollRepository.deleteById(pollId);

                // 清理相关缓存
                String voteCountKey = VOTE_COUNT_KEY + pollId;
                redisTemplate.delete(voteCountKey);

                // 清理用户投票记录缓存
                Set<String> keys = redisTemplate.keys("user_votes:*");
                if (keys != null) {
                        keys.forEach(key -> redisTemplate.opsForHash().delete(key, pollId.toString()));
                }
        }

        private void validatePageNumberAndSize(int page, int size) {
                if (page < 0) {
                        throw new BadRequestException("Page number cannot be less than zero.");
                }

                if (size > AppConstants.MAX_PAGE_SIZE) {
                        throw new BadRequestException(
                                        "Page size must not be greater than " + AppConstants.MAX_PAGE_SIZE);
                }
        }

        private Map<Long, Long> getChoiceVoteCountMap(List<Long> pollIds) {
                // Retrieve Vote Counts of every Choice belonging to the given pollIds
                List<ChoiceVoteCount> votes = voteRepository.countByPollIdInGroupByChoiceId(pollIds);

                Map<Long, Long> choiceVotesMap = votes.stream()
                                .collect(Collectors.toMap(ChoiceVoteCount::getChoiceId, ChoiceVoteCount::getVoteCount));

                return choiceVotesMap;
        }

        private Map<Long, Long> getPollUserVoteMap(UserPrincipal currentUser, List<Long> pollIds) {
                // Retrieve Votes done by the logged in user to the given pollIds
                Map<Long, Long> pollUserVoteMap = null;
                if (currentUser != null) {
                        List<Vote> userVotes = voteRepository.findByUserIdAndPollIdIn(currentUser.getId(), pollIds);

                        pollUserVoteMap = userVotes.stream()
                                        .collect(Collectors.toMap(vote -> vote.getPoll().getId(),
                                                        vote -> vote.getChoice().getId()));
                }
                return pollUserVoteMap;
        }

        private Map<Long, User> getPollCreatorMap(List<Poll> polls) {
                // Get Poll Creator details of the given list of polls
                List<Long> creatorIds = polls.stream()
                                .map(Poll::getCreatedBy)
                                .distinct()
                                .collect(Collectors.toList());

                List<User> creators = userRepository.findByIdIn(creatorIds);
                Map<Long, User> creatorMap = creators.stream()
                                .collect(Collectors.toMap(User::getId, Function.identity()));

                return creatorMap;
        }
}
