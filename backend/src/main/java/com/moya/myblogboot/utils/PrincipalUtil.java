package com.moya.myblogboot.utils;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.security.Principal;

public final class PrincipalUtil {

    private PrincipalUtil() {}

    public static Long getMemberId(Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken token) {
            return (Long) token.getPrincipal();
        }
        return -1L;
    }
}
