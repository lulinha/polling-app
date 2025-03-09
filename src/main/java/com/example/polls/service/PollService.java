package com.example.polls.service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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

import com.example.polls.constants.AppConstants;
import com.example.polls.constants.RedisConstants;
import com.example.polls.dto.PollDTO;
import com.example.polls.exception.BadRequestException;
import com.example.polls.exception.ResourceNotFoundException;
import com.example.polls.model.Category;
import com.example.polls.model.Choice;
import com.example.polls.model.ChoiceVoteCount;
import com.example.polls.model.Poll;
import com.example.polls.model.Tag;
import com.example.polls.model.User;
import com.example.polls.model.Vote;
import com.example.polls.payload.PagedResponse;
import com.example.polls.payload.PollRequest;
import com.example.polls.payload.PollResponse;
import com.example.polls.repository.CategoryRepository;
import com.example.polls.repository.PollRepository;
import com.example.polls.repository.TagRepository;
import com.example.polls.repository.UserRepository;
import com.example.polls.repository.VoteRepository;
import com.example.polls.security.UserPrincipal;
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
        private UserService userService;

        @Autowired
        private KafkaProducerService kafkaProducerService;

        @Autowired
        private NotificationService notificationService;

        @Autowired
        private CategoryRepository categoryRepository;

        @Autowired
        private TagRepository tagRepository;

        @Autowired
        private RedisTemplate<String, Object> redisTemplate;

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
        public Poll createPoll(PollRequest pollRequest, UserPrincipal currentUser) {
                Poll poll = new Poll();
                poll.setQuestion(pollRequest.getQuestion());

                pollRequest.getChoices().forEach(choiceRequest -> {
                        poll.addChoice(new Choice(choiceRequest.getText()));
                });

                Instant now = Instant.now();
                Instant expirationDateTime = now.plus(Duration.ofDays(pollRequest.getPollLength().getDays()))
                                .plus(Duration.ofHours(pollRequest.getPollLength().getHours()));

                poll.setExpirationDateTime(expirationDateTime);
                // 处理分类
                Set<Category> categories = pollRequest.getCategoryIds().stream()
                                .map(categoryRepository::findById) // 转换为 Optional<Category> 流
                                .filter(Optional::isPresent) // 过滤空 Optional
                                .map(Optional::get) // 解包 Category 对象
                                .collect(Collectors.toSet()); // 收集为 Set

                // 最佳实践：使用 flatMap 处理 Optional
                /*
                 * Set<Category> categories = pollRequest.getCategoryIds().stream()
                 * .flatMap(id -> categoryRepository.findById(id).stream())
                 * .collect(Collectors.toSet());
                 */
                poll.setCategories(categories);

                // 处理标签
                Set<Tag> tags = pollRequest.getTagNames().stream()
                                .map(tagRepository::findByName) // 转换为 Optional<Tag> 流
                                .filter(Optional::isPresent)
                                .map(Optional::get)
                                .collect(Collectors.toSet());

                /*
                 * 扩展功能建议： （如果需要自动创建不存在的标签）
                 * Set<Tag> tags = pollRequest.getTagNames().stream()
                 * .map(name -> tagRepository.findByName(name)
                 * .orElseGet(() -> tagRepository.save(new Tag(name)))
                 * .collect(Collectors.toSet());
                 */

                /*
                 * 最佳实践：使用 flatMap 处理 Optional
                 * Set<Tag> tags = pollRequest.getTagNames().stream()
                 * .flatMap(name -> tagRepository.findByName(name).stream())
                 * .collect(Collectors.toSet());
                 */
                poll.setTags(tags);

                // 保存投票
                Poll savedPoll = pollRepository.save(poll);

                // 获取当前用户
                User user = userRepository.findById(currentUser.getId())
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "User with id " + currentUser.getId() + " not found"));

                // 为用户增加积分，这里假设创建投票加 10 积分，可按需调整
                userService.addPoints(user, 10);

                // 使用持久化发送
                PollDTO pollDTO = ModelMapper.convertToPollDTO(savedPoll);
                String message = JsonUtils.toJson(pollDTO);
                logger.info("Sending message: {}", message);
                kafkaProducerService.sendWithPersistence("poll-created", savedPoll.getId().toString(), message);

                return savedPoll;
        }

        @Transactional
        @CacheEvict(allEntries = true) // 创建新投票时清除所有缓存（根据业务需求调整）
        public Poll createPollWithCache(PollRequest pollRequest, UserPrincipal currentUser) {
                Poll poll = new Poll();
                poll.setQuestion(pollRequest.getQuestion());

                pollRequest.getChoices().forEach(choiceRequest -> {
                        poll.addChoice(new Choice(choiceRequest.getText()));
                });

                Instant now = Instant.now();
                Instant expirationDateTime = now.plus(Duration.ofDays(pollRequest.getPollLength().getDays()))
                                .plus(Duration.ofHours(pollRequest.getPollLength().getHours()));

                poll.setExpirationDateTime(expirationDateTime);
                // 处理分类
                Set<Category> categories = pollRequest.getCategoryIds().stream()
                                .map(categoryRepository::findById) // 转换为 Optional<Category> 流
                                .filter(Optional::isPresent) // 过滤空 Optional
                                .map(Optional::get) // 解包 Category 对象
                                .collect(Collectors.toSet()); // 收集为 Set
                poll.setCategories(categories);

                // 处理标签
                Set<Tag> tags = pollRequest.getTagNames().stream()
                                .map(tagRepository::findByName) // 转换为 Optional<Tag> 流
                                .filter(Optional::isPresent)
                                .map(Optional::get)
                                .collect(Collectors.toSet());
                poll.setTags(tags);

                // 保存投票
                Poll savedPoll = pollRepository.save(poll);

                // 获取当前用户
                User user = userRepository.findById(currentUser.getId())
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "User with id " + currentUser.getId() + " not found"));

                // 为用户增加积分，这里假设创建投票加 10 积分，可按需调整
                userService.addPoints(user, 10);

                // 创建投票时初始化热度分数为0
                redisTemplate.opsForZSet().add(RedisConstants.HOT_POLLS_KEY, savedPoll.getId(), 0);

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
                String key = RedisConstants.VOTE_COUNT_KEY + poll.getId();
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
                String key = RedisConstants.VOTE_COUNT_KEY + pollId;
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
                Poll poll = pollRepository.findById(pollId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Poll with id " + pollId + " not found"));
                pollRepository.delete(poll);

                // 通知相关用户
                notificationService.notifyUserAboutPollDeletion(pollId);
        }

        @CacheEvict(key = "#pollId")
        public void deletePollWithCache(Long pollId) {
                // 删除数据库记录
                pollRepository.deleteById(pollId);

                // 清理相关缓存
                String voteCountKey = RedisConstants.VOTE_COUNT_KEY + pollId;
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

        // 添加获取热门投票的方法
        public PagedResponse<PollResponse> getHotPolls(UserPrincipal currentUser) {
                // 获取热门投票ID（Redis ZSET）
                Set<Long> pollIds = getHotPollIdsFromCache();

                // 缓存未命中时的兜底策略
                if (pollIds.isEmpty()) {
                        pollIds = getFallbackPollIds();
                }

                // 获取投票详情并保持顺序
                List<PollResponse> pollResponses = getOrderedPollResponses(pollIds);

                // 构建分页响应（移至Service层）
                return buildPagedResponse(pollResponses);
        }

        // 私有辅助方法（模块化拆分）
        private Set<Long> getHotPollIdsFromCache() {
                return Optional.ofNullable(
                                redisTemplate.opsForZSet()
                                                .reverseRange(RedisConstants.HOT_POLLS_KEY, 0,
                                                                RedisConstants.HOT_POLLS_LIMIT - 1))
                                .orElseGet(Collections::emptySet)
                                .stream()
                                .map(id -> ((Number) id).longValue())
                                .collect(Collectors.toSet());
        }

        private Set<Long> getFallbackPollIds() {
                return pollRepository.findRecentActivePolls(RedisConstants.HOT_POLLS_LIMIT)
                                .stream()
                                .map(Poll::getId)
                                .collect(Collectors.toSet());
        }

        private List<PollResponse> getOrderedPollResponses(Set<Long> pollIds) {
                if (pollIds.isEmpty()) {
                        return Collections.emptyList();
                }

                List<Poll> polls = pollRepository.findByIdIn(new ArrayList<>(pollIds));
                Map<Long, Poll> pollMap = polls.stream()
                                .collect(Collectors.toMap(Poll::getId, Function.identity()));

                return pollIds.stream()
                                .map(pollMap::get)
                                .filter(Objects::nonNull)
                                .map(this::convertToPollResponse)
                                .collect(Collectors.toList());
        }

        private PollResponse convertToPollResponse(Poll poll) {
                return ModelMapper.mapPollToPollResponse(
                                poll,
                                getChoiceVoteCountMap(Collections.singletonList(poll.getId())),
                                userRepository.getReferenceById(poll.getCreatedBy()), // 使用正确方法
                                null);
        }

        private PagedResponse<PollResponse> buildPagedResponse(List<PollResponse> content) {
                int totalElements = content.size();
                return new PagedResponse<>(
                                content,
                                0, // page
                                RedisConstants.HOT_POLLS_LIMIT, // size（使用统一常量）
                                totalElements,
                                calculateTotalPages(totalElements),
                                isLastPage(totalElements));
        }

        private int calculateTotalPages(int totalElements) {
                return (int) Math.ceil((double) totalElements / RedisConstants.HOT_POLLS_LIMIT);
        }

        private boolean isLastPage(int totalElements) {
                return totalElements <= RedisConstants.HOT_POLLS_LIMIT;
        }

        public Poll approvePoll(Long pollId, boolean approved) {
                Poll poll = pollRepository.findById(pollId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Poll with id " + pollId + " not found"));
                poll.setApproved(approved);
                Poll savedPoll = pollRepository.save(poll);

                // 通知相关用户
                if (!approved) {
                        // 假设这里有一个通知用户的方法
                        notificationService.notifyUserAboutPollRejection(pollId);
                }
                return savedPoll;
        }
}
