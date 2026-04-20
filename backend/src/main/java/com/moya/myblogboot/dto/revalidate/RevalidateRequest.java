package com.moya.myblogboot.dto.revalidate;

import java.util.List;

public record RevalidateRequest(List<String> tags, List<String> paths) {

    public static RevalidateRequest of(List<String> tags, List<String> paths) {
        return new RevalidateRequest(
                tags == null ? List.of() : tags,
                paths == null ? List.of() : paths
        );
    }
}
