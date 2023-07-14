package com.moya.myblogboot.domain.admin;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@NoArgsConstructor
public class AdminReqDto {
    @NotBlank(message = "아이디를 입력하세요.")
    private String username;
    @NotBlank(message = "비밀번호를 입력하세요.")
    private String password;

    @Builder
    public AdminReqDto(String username, String password) {
        this.username = username;
        this.password = password;

    }
}
