package com.moya.myblogboot.domain.guest;

import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class GuestReqDto {
    @NotEmpty(message = "아이디를 입력하세요.")
    private String username;
    @NotEmpty(message = "비밀번호 4-8자리를 입력하세요.")
    private String password;

    @Builder
    public GuestReqDto(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public Guest toEntity() {
        Guest guest = Guest.builder()
                .username(this.username)
                .password(this.password)
                .build();
        return guest;
    }

    public void passwordEncode(String encodedPw) {
        this.password = encodedPw;
    }
}
