package com.moya.myblogboot.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginReqDto {
    @NotBlank(message = "아이디를 입력하세요.")
    @Size(min = 6, max = 20, message = "아이디는 6자 이상 20자 이하로 입력하여야 합니다.")
    private String username;
    @NotBlank(message = "비밀번호를 입력하세요.")
    @Size(min = 8, max = 16, message = "비밀번호는 8자 이상 16자 이하로 입력하여야 합니다.")
    private String password;
}
