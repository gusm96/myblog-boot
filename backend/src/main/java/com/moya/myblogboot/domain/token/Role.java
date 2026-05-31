package com.moya.myblogboot.domain.token;

import java.util.Arrays;

public enum Role {
    ADMIN("ROLE_ADMIN");

    private final String authority;

    Role(String authority) {
        this.authority = authority;
    }

    public String getAuthority() {
        return authority;
    }

    public String displayName() {
        return name();
    }

    public static Role fromAuthority(String authority) {
        return Arrays.stream(values())
                .filter(role -> role.authority.equals(authority))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown authority: " + authority));
    }
}
