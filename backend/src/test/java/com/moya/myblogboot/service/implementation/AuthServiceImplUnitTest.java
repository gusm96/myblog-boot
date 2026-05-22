package com.moya.myblogboot.service.implementation;

import com.moya.myblogboot.domain.admin.Admin;
import com.moya.myblogboot.domain.login.LoginAttemptResult;
import com.moya.myblogboot.dto.auth.LoginReqDto;
import com.moya.myblogboot.exception.ErrorCode;
import com.moya.myblogboot.exception.custom.UnauthorizedException;
import com.moya.myblogboot.repository.AdminRepository;
import com.moya.myblogboot.service.LoginAttemptService;
import com.moya.myblogboot.service.RefreshTokenService;
import com.moya.myblogboot.utils.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.anyString;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplUnitTest {

    @Mock
    private AdminRepository adminRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private LoginAttemptService loginAttemptService;
    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("Login failures use the same error code")
    void adminLoginFailureUsesSameErrorCode() {
        AuthCredentialVerifier authCredentialVerifier = new AuthCredentialVerifier(adminRepository, passwordEncoder);
        AuthServiceImpl authService = new AuthServiceImpl(refreshTokenService, loginAttemptService,
                authCredentialVerifier, jwtTokenProvider);
        LoginReqDto notExistsUsername = LoginReqDto.builder()
                .username("notExists")
                .password("testPassword")
                .build();
        LoginReqDto wrongPassword = LoginReqDto.builder()
                .username("admin")
                .password("wrongPassword")
                .build();
        Admin admin = Admin.builder()
                .username("admin")
                .password("encodedPassword")
                .build();

        given(adminRepository.findByUsername("notExists")).willReturn(Optional.empty());
        given(adminRepository.findByUsername("admin")).willReturn(Optional.of(admin));
        given(passwordEncoder.matches("wrongPassword", "encodedPassword")).willReturn(false);
        given(loginAttemptService.onFailure(anyString(), anyString()))
                .willReturn(new LoginAttemptResult(1, 0L, false));

        assertInvalidCredentials(authService, notExistsUsername);
        assertInvalidCredentials(authService, wrongPassword);
    }

    private void assertInvalidCredentials(AuthServiceImpl authService, LoginReqDto loginReqDto) {
        assertThatThrownBy(() -> authService.adminLogin(loginReqDto))
                .isInstanceOfSatisfying(UnauthorizedException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_CREDENTIALS));
    }
}
