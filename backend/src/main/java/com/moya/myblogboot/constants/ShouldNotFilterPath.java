package com.moya.myblogboot.constants;

import java.util.Arrays;
import java.util.List;

public class ShouldNotFilterPath {
    public static final List<String> EXCLUDE_PATHS = Arrays.asList(
            "/api/v1/login",
            "/api/v1/logout",
            "/api/v1/reissuing-token",
            "/api/v1/posts/search",
            "/api/v2/categories",
            "/api/v8/posts",
            "/api/v2/visitor-count"
    );
}
