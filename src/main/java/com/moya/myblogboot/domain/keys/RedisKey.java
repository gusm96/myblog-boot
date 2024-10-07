package com.moya.myblogboot.domain.keys;

public class RedisKey {
    public static final String BOARD_KEY = "board:";
    public static final String BOARD_VIEWS_KEY = ":views";
    public static final String BOARD_LIKES_KEY = ":likes";
    public static final String RANDOM_USER_NUM_KEY = "randomUserNum:";
    public static final String USER_VIEWED_BOARD_KEY = "userViewedBoards:";
    public static final String VISITOR_COUNT_KEY = "visitorCount:";

    public static final String TODAY_COUNT_KEY = "today";
    public static final String TOTAL_COUNT_KEY = "total";
    public static final String YESTERDAY_COUNT_KEY = "yesterday";
}
