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

    public static final String LOGIN_FAIL_ACCOUNT_KEY = "login:{acct:%s}:fail";
    public static final String LOGIN_LOCK_ACCOUNT_KEY = "login:{acct:%s}:lock";
    public static final String LOGIN_FAIL_IP_KEY = "login:{ip:%s}:fail";
    public static final String LOGIN_LOCK_IP_KEY = "login:{ip:%s}:lock";
}
