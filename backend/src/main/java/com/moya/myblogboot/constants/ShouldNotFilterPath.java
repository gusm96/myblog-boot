package com.moya.myblogboot.constants;

import java.util.Arrays;
import java.util.List;

public class ShouldNotFilterPath {
    public static final List<String> EXCLUDE_PATHS = Arrays.asList(
            "/api/v1/join",
            "/api/v1/login",
            "/api/v1/logout",
            "/api/v2/boards",
            "/api/v3/boards",
            "/api/v4/boards",
            "/api/v5/boards",
            "/api/v6/boards",
            "/api/v7/boards",
            "/api/v1/boards/search",
            "/api/v1/reissuing-token",
            "/api/v1/password-strength-check"
    );
}
