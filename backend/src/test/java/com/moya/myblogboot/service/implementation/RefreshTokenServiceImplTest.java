package com.moya.myblogboot.service.implementation;

import com.moya.myblogboot.AbstractContainerBaseTest;
import com.moya.myblogboot.domain.admin.Admin;
import com.moya.myblogboot.domain.token.IssuedToken;
import com.moya.myblogboot.domain.token.RefreshTokenClaims;
import com.moya.myblogboot.domain.token.ReissuedToken;
import com.moya.myblogboot.exception.custom.InvalidateTokenException;
import com.moya.myblogboot.repository.RefreshTokenRedisRepository;
import com.moya.myblogboot.service.RefreshTokenService;
import com.moya.myblogboot.utils.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class RefreshTokenServiceImplTest extends AbstractContainerBaseTest {

    @Autowired
    private RefreshTokenService refreshTokenService;
    @Autowired
    private RefreshTokenRedisRepository refreshTokenRedisRepository;
    @Value("${jwt.secret}")
    private String secret;

    @Test
    @DisplayName("Login issue creates refresh token with server-side family")
    void issueOnLogin() {
        IssuedToken issuedToken = refreshTokenService.issueOnLogin(admin());

        RefreshTokenClaims claims = JwtUtil.parseRefreshToken(issuedToken.refreshToken(), secret);

        assertThat(claims.jti()).isNotBlank();
        assertThat(claims.familyId()).isNotBlank();
        assertThat(refreshTokenRedisRepository.findAbsoluteExpiry(claims.familyId())).isPresent();
    }

    @Test
    @DisplayName("Refresh token rotates and old token is accepted only within grace window")
    void rotateWithGraceWindow() {
        IssuedToken issuedToken = refreshTokenService.issueOnLogin(admin());

        ReissuedToken first = refreshTokenService.rotate(issuedToken.refreshToken());
        ReissuedToken grace = refreshTokenService.rotate(issuedToken.refreshToken());

        assertThat(first).isEqualTo(grace);
        assertThat(JwtUtil.parseRefreshToken(first.refreshToken(), secret).jti())
                .isNotEqualTo(JwtUtil.parseRefreshToken(issuedToken.refreshToken(), secret).jti());
    }

    @Test
    @DisplayName("Logout revokes refresh token family")
    void revokeOnLogout() {
        IssuedToken issuedToken = refreshTokenService.issueOnLogin(admin());
        RefreshTokenClaims claims = JwtUtil.parseRefreshToken(issuedToken.refreshToken(), secret);

        refreshTokenService.revokeOnLogout(issuedToken.refreshToken());

        assertThat(refreshTokenRedisRepository.findFamilyReason(claims.familyId())).contains("LOGOUT");
        assertThatThrownBy(() -> refreshTokenService.rotate(issuedToken.refreshToken()))
                .isInstanceOf(InvalidateTokenException.class);
    }

    private Admin admin() {
        Admin admin = Admin.builder()
                .username("admin")
                .password("password")
                .build();
        ReflectionTestUtils.setField(admin, "id", 1L);
        return admin;
    }
}
