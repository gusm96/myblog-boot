package com.moya.myblogboot.service.implementation;

import com.moya.myblogboot.AbstractContainerBaseTest;
import com.moya.myblogboot.domain.admin.Admin;
import com.moya.myblogboot.domain.member.MemberLoginReqDto;
import com.moya.myblogboot.domain.token.Token;
import com.moya.myblogboot.exception.custom.EntityNotFoundException;
import com.moya.myblogboot.exception.custom.UnauthorizedException;
import com.moya.myblogboot.repository.AdminRepository;
import com.moya.myblogboot.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@Transactional
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthServiceImplTest extends AbstractContainerBaseTest {

    @Autowired
    private AuthService authService;
    @Autowired
    private AdminRepository adminRepository;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void before() {
        Admin admin = Admin.builder()
                .username("testMember")
                .password(passwordEncoder.encode("testPassword"))
                .build();
        adminRepository.save(admin);
    }

    @Test
    @DisplayName("관리자 로그인 테스트")
    void adminLogin() {
        MemberLoginReqDto notExistsUsername = MemberLoginReqDto.builder()
                .username("notExists")
                .password("test1234")
                .build();
        MemberLoginReqDto wrongPassword = MemberLoginReqDto.builder()
                .username("testMember")
                .password("wrongPw")
                .build();
        MemberLoginReqDto validLogin = MemberLoginReqDto.builder()
                .username("testMember")
                .password("testPassword")
                .build();

        assertThrows(EntityNotFoundException.class, () -> authService.adminLogin(notExistsUsername));
        assertThrows(UnauthorizedException.class, () -> authService.adminLogin(wrongPassword));
        Object result = authService.adminLogin(validLogin);
        assertTrue(result instanceof Token);
    }

    @Test
    @DisplayName("Access Token 재발급 테스트")
    void reissuingAccessToken() {
        MemberLoginReqDto loginReqDto = MemberLoginReqDto.builder()
                .username("testMember")
                .password("testPassword")
                .build();

        Token token = authService.adminLogin(loginReqDto);
        String result = authService.reissuingAccessToken(token.getRefresh_token());

        assertTrue(result instanceof String);
    }

    @Test
    @DisplayName("임시번호 생성")
    void createTemporaryNumber() {
        String temporaryNumber = String.valueOf(ThreadLocalRandom.current().nextLong());

        while (redisTemplate.opsForValue().get(temporaryNumber) != null) {
            temporaryNumber = String.valueOf(ThreadLocalRandom.current().nextLong());
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yy-MM-dd HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        String formattedNow = now.format(formatter);

        LocalDateTime midnight = now.plusDays(1).truncatedTo(ChronoUnit.DAYS);
        long ttl = ChronoUnit.SECONDS.between(now, midnight);

        redisTemplate.opsForValue().set(temporaryNumber, formattedNow, ttl, TimeUnit.SECONDS);
    }
}
