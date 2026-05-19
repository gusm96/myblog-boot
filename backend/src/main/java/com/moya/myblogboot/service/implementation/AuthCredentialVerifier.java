package com.moya.myblogboot.service.implementation;

import com.moya.myblogboot.domain.admin.Admin;
import com.moya.myblogboot.dto.auth.LoginReqDto;
import com.moya.myblogboot.exception.ErrorCode;
import com.moya.myblogboot.exception.custom.UnauthorizedException;
import com.moya.myblogboot.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthCredentialVerifier {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public Admin verify(LoginReqDto loginReqDto) {
        Admin admin = adminRepository.findByUsername(loginReqDto.getUsername())
                .orElseThrow(() -> {
                    log.warn("Admin login failed: username not found");
                    return new UnauthorizedException(ErrorCode.INVALID_CREDENTIALS);
                });
        if (!passwordEncoder.matches(loginReqDto.getPassword(), admin.getPassword())) {
            log.warn("Admin login failed: invalid password");
            throw new UnauthorizedException(ErrorCode.INVALID_CREDENTIALS);
        }
        return admin;
    }
}
