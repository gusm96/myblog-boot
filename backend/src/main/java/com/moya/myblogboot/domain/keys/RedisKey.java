package com.moya.myblogboot.domain.keys;

public class RedisKey {
    public static final String POST_KEY = "post:";
    public static final String POST_VIEWS_KEY = ":views";
    public static final String POST_LIKES_KEY = ":likes";
    public static final String USER_VIEWED_POST_KEY = "userViewedPosts:";
    public static final String VISITOR_COUNT_KEY = "visitorCount:";

    public static final String TODAY_COUNT_KEY = "today";
    public static final String TOTAL_COUNT_KEY = "total";
    public static final String YESTERDAY_COUNT_KEY = "yesterday";

    public static final String REFRESH_FAMILY_KEY = "refresh:{%s}:family";
    public static final String REFRESH_TOKEN_KEY = "refresh:{%s}:token:%s";
    public static final String REFRESH_ROTATION_RESPONSE_KEY = "refresh:{%s}:rotation-response:%s";
}
