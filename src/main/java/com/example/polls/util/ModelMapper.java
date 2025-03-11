package com.example.polls.util;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;

import com.example.polls.dto.ChoiceDTO;
import com.example.polls.dto.PollDTO;
import com.example.polls.model.Comment;
import com.example.polls.model.Poll;
import com.example.polls.model.User;
import com.example.polls.payload.ChoiceResponse;
import com.example.polls.payload.CommentResponse;
import com.example.polls.payload.PollResponse;
import com.example.polls.payload.UserSummary;

public class ModelMapper {

    // 处理 getReferenceById() 的延迟加载, 如果 ModelMapper.mapPollToPollResponse
    // 中需要访问用户详细信息（如用户名、邮箱），
    // 需确保在事务上下文中使用 @Transactional 注解，否则可能触发 LazyInitializationException：
    @Transactional(readOnly = true)
    public static PollResponse mapPollToPollResponse(Poll poll, Map<Long, Long> choiceVotesMap, User creator,
            Long userVote) {
        PollResponse pollResponse = new PollResponse();
        pollResponse.setId(poll.getId());
        pollResponse.setQuestion(poll.getQuestion());
        pollResponse.setCreationDateTime(poll.getCreatedAt());
        pollResponse.setExpirationDateTime(poll.getExpirationDateTime());
        Instant now = Instant.now();
        pollResponse.setExpired(poll.getExpirationDateTime().isBefore(now));

        List<ChoiceResponse> choiceResponses = poll.getChoices().stream().map(choice -> {
            ChoiceResponse choiceResponse = new ChoiceResponse();
            choiceResponse.setId(choice.getId());
            choiceResponse.setText(choice.getText());

            if (choiceVotesMap.containsKey(choice.getId())) {
                choiceResponse.setVoteCount(choiceVotesMap.get(choice.getId()));
            } else {
                choiceResponse.setVoteCount(0);
            }
            return choiceResponse;
        }).collect(Collectors.toList());

        pollResponse.setChoices(choiceResponses);
        UserSummary creatorSummary = new UserSummary(creator.getId(), creator.getUsername(), creator.getName());
        pollResponse.setCreatedBy(creatorSummary);

        if (userVote != null) {
            pollResponse.setSelectedChoice(userVote);
        }

        long totalVotes = pollResponse.getChoices().stream().mapToLong(ChoiceResponse::getVoteCount).sum();
        pollResponse.setTotalVotes(totalVotes);

        return pollResponse;
    }

    public static PollDTO convertToPollDTO(Poll poll) {
        PollDTO dto = new PollDTO();
        dto.setId(poll.getId());
        dto.setQuestion(poll.getQuestion());
        dto.setExpirationDateTime(poll.getExpirationDateTime());

        List<ChoiceDTO> choiceDTOs = poll.getChoices().stream()
                .map(choice -> {
                    ChoiceDTO choiceDTO = new ChoiceDTO();
                    choiceDTO.setId(choice.getId());
                    choiceDTO.setText(choice.getText());
                    return choiceDTO;
                })
                .collect(Collectors.toList());
        dto.setChoices(choiceDTOs);

        return dto;
    }

    public CommentResponse toResponse(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getContent(),
                comment.getUser().getUsername(),
                comment.getCreatedAt(),
                comment.getLikeCount(),
                comment.getParent() != null ? comment.getParent().getId() : null,
                comment.getReplies().stream()
                .map(this::toResponse)
                .toList());
    }

}
