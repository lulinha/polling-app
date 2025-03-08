package com.example.polls.constants;

public final class AppConstants {

    // 私有构造函数防止实例化
    private AppConstants() {
        throw new AssertionError("Cannot instantiate constants class");
    }

    public static final String DEFAULT_PAGE_NUMBER = "0";
    public static final String DEFAULT_PAGE_SIZE = "10";
    public static final int MAX_PAGE_SIZE = 50;

}
