package com.moya.myblogboot;

import com.moya.myblogboot.domain.member.MemberJoinReqDto;
import com.moya.myblogboot.exception.DuplicateUsernameException;
import com.moya.myblogboot.repository.MemberRepository;
import com.moya.myblogboot.service.AuthService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class GlobalExceptionTest {

    @Autowired
    private AuthService authService;


    @DisplayName("아이디 중복 테스트")
    @Test
    void 아이디_중복() {
        // 아이디 등록
        MemberJoinReqDto newMember1 = MemberJoinReqDto.builder()
                .username("moyada123")
                .password("moyada123")
                .nickname("moyada").build();

        MemberJoinReqDto newMember2 = MemberJoinReqDto.builder()
                .username("moyada123")
                .password("moyada123")
                .nickname("moyada").build();
        authService.memberJoin(newMember1);

        Assertions.assertThrows(DuplicateUsernameException.class, () -> {
            authService.memberJoin(newMember2);
        });
    }
}
