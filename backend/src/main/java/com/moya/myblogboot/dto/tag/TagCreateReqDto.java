package com.moya.myblogboot.dto.tag;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TagCreateReqDto(@NotBlank @Size(max = 40) String name) {
}
