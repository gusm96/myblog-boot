package com.moya.myblogboot.service.implementation;

import com.moya.myblogboot.domain.member.Member;
import com.moya.myblogboot.domain.member.MemberJoinReqDto;
import com.moya.myblogboot.domain.member.MemberLoginReqDto;
import com.moya.myblogboot.domain.token.Token;
import com.moya.myblogboot.repository.MemberRepository;
import com.moya.myblogboot.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.assertj.core.api.Assertions.*;

@Transactional
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthServiceImplTest {
    @Autowired
    private AuthService authService;
    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void before() {
        Member member = Member.builder()
                .username("testMember")
                .password(passwordEncoder.encode("testPassword"))
                .nickname("tester")
                .build();
        memberRepository.save(member);
    }
    @Test
    @DisplayName("회원 가입 테스트")
    void memberJoin() {
        // 중복되지 않은 회원
        MemberJoinReqDto reqMember1 = MemberJoinReqDto.builder()
                .username("test1234")
                .password("test1234")
                .nickname("test1234")
                .build();
        
        // 중복된 회원
        MemberJoinReqDto reqMember2 = MemberJoinReqDto.builder()
                .username("testMember")
                .password("test1234")
                .nickname("test1234")
                .build();

        // 회원이 중복되었을 때
        assertThrows(DuplicateKeyException.class, () -> authService.memberJoin(reqMember2));
        String result = authService.memberJoin(reqMember1);
        assertEquals(result, "회원가입을 성공했습니다.");
    }

    @Test
    @DisplayName("회원 로그인 테스트")
    void memberLogin() {
        // 존재하지 않는 회원 아이디
        MemberLoginReqDto notExistsUsername = MemberLoginReqDto.builder()
                .username("test1234")
                .password("test1234")
                .build();
        // 회원 아이디가 존재하지만 비밀번호가 틀린 경우
        MemberLoginReqDto notEqualsPassword = MemberLoginReqDto.builder()
                .username("testMember")
                .password("notEqualsPw")
                .build();
        // 존재하는 회원
        MemberLoginReqDto memberLoginReqDto = MemberLoginReqDto.builder()
                .username("testMember")
                .password("testPassword")
                .build();
        assertThrows(UsernameNotFoundException.class, () -> authService.memberLogin(notExistsUsername));
        assertThrows(BadCredentialsException.class, () -> authService.memberLogin(notEqualsPassword));
        Object result = authService.memberLogin(memberLoginReqDto);
        assertTrue(result instanceof Token);
    }

    @Test
    @DisplayName("Id값으로 DB에서 회원 찾기")
    void retrieveMemberById() {
        Member newMember = Member.builder()
                .username("newMember")
                .password("newMemberPw")
                .nickname("newMember")
                .build();
        Member saveMember = memberRepository.save(newMember);

        Member findMember = authService.retrieveMemberById(saveMember.getId());
        assertThat(findMember).isEqualTo(saveMember);
    }

    @Test
    @DisplayName("Access Token 재발급 테스트")
    void reissuingAccessToken() {
        MemberLoginReqDto memberLoginReqDto = MemberLoginReqDto.builder()
                .username("testMember")
                .password("testPassword")
                .build();

        Token token = authService.memberLogin(memberLoginReqDto);
        String result = authService.reissuingAccessToken(token.getRefresh_token());

        assertTrue(result instanceof String);
    }

}