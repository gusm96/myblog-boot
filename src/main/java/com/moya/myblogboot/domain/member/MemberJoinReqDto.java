package com.moya.myblogboot.domain.member;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MemberJoinReqDto {

    // 사용자 아이디
    @NotBlank(message = "6-20자의 영문 소문자, 숫자를 조합하여 입력하세요.")
    @Size(min = 6, max = 20, message = "6-20자의 영문 소문자, 숫자를 조합하여 입력하세요.")
    private String username;

    // 사용자 비밀번호
    @NotBlank(message = "8~16자의 영문 대/소문자, 숫자, 특수기호를 조합하여 입력하세요.")
    @Size(min = 8, max = 16, message = "8~16자의 영문 대/소문자, 숫자, 특수기호를 조합하여 입력하세요.")
    private String password;


    // 사용자 닉네임
    @NotBlank(message = "2~8글자 사이의 닉네임을 입력하세요")
    @Size(min = 2, max = 8, message = "2~8글자 사이의 닉네임을 입력하세요")
    private String nickname;

    public Member toEntity() {
        Member member = Member.builder()
                .username(this.getUsername())
                .password(this.getPassword())
                .nickname(this.getNickname())
                .build();
        return member;
    }

    public void passwordEncode (String encodedPassword){
        this.password = encodedPassword;
    }
}
