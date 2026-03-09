package com.moya.myblogboot.domain.member;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberLoginReqDto {
    @NotBlank(message = "아이디를 입력하세요.")
    @Size(min = 6, max = 20, message = "아이디는 6자 이상 20자 이하로 입력하여야 합니다.\n※ 공백 및 특수문자 입력 불가능.")
    private String username;
    @NotBlank(message = "비밀번호를 입력하세요.")
    @Size(min = 8, max = 16, message = "비밀번호는 8자 이상 16 이하로 입력하여야 합니다.\n※ 공백 및 \'!@#$%\'를 제외한 특수문자 입력 불가능")
    private String password;
}

