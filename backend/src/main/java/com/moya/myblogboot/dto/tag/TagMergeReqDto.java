package com.moya.myblogboot.dto.tag;

import jakarta.validation.constraints.NotNull;

public record TagMergeReqDto(@NotNull Long srcId, @NotNull Long dstId) {
}
