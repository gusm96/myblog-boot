package com.moya.myblogboot.service.implementation;

import com.moya.myblogboot.domain.admin.Admin;
import com.moya.myblogboot.dto.auth.LoginReqDto;
import com.moya.myblogboot.exception.ErrorCode;
import com.moya.myblogboot.exception.custom.UnauthorizedException;
import com.moya.myblogboot.repository.AdminRepository;
import com.moya.myblogboot.service.LoginAttemptService;
import com.moya.myblogboot.service.RefreshTokenService;
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

    @Test
    @DisplayName("Login failures use the same error code")
    void adminLoginFailureUsesSameErrorCode() {
        AuthCredentialVerifier authCredentialVerifier = new AuthCredentialVerifier(adminRepository, passwordEncoder);
        AuthServiceImpl authService = new AuthServiceImpl(refreshTokenService, loginAttemptService, authCredentialVerifier);
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

        assertInvalidCredentials(authService, notExistsUsername);
        assertInvalidCredentials(authService, wrongPassword);
    }

    private void assertInvalidCredentials(AuthServiceImpl authService, LoginReqDto loginReqDto) {
        assertThatThrownBy(() -> authService.adminLogin(loginReqDto))
                .isInstanceOfSatisfying(UnauthorizedException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_CREDENTIALS));
    }
}
