package com.example.polls.constants;

public final class RedisConstants {
    private RedisConstants() {} // 防止实例化

    public static final String HOT_POLLS = "polls:hot";
    public static final String VOTE_COUNT = "polls:votes:count";
    public static final String VOTE_COUNT_KEY = "poll_votes:";
    public static final String HOT_POLLS_KEY = "hot_polls";
    public static final int HOT_POLLS_LIMIT = 10;
}