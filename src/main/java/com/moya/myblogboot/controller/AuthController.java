package com.moya.myblogboot.controller;

import com.moya.myblogboot.domain.admin.AdminReqDto;
import com.moya.myblogboot.domain.guest.GuestReqDto;
import com.moya.myblogboot.domain.token.Token;
import com.moya.myblogboot.domain.token.TokenUserType;
import com.moya.myblogboot.exception.ExpiredTokenException;
import com.moya.myblogboot.service.AuthService;
import jakarta.persistence.NoResultException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // 게스트 로그인
    @PostMapping("/api/v1/login/guest")
    public ResponseEntity<?> loginGuest(@RequestBody @Valid GuestReqDto guestReqDto) {
        try {
            Token token = authService.guestLogin(guestReqDto);
            return ResponseEntity.ok().body(token);
        } catch (NoResultException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (BadCredentialsException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }
    // 관리자 로그인
    @PostMapping("/api/v1/login/admin")
    public ResponseEntity<?> adminLogin(@RequestBody @Valid AdminReqDto adminReqDto) {
        try {
            Token token = authService.adminLogin(adminReqDto.getUsername(), adminReqDto.getPassword());
            return ResponseEntity.ok().body(token);
        } catch (NoResultException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (BadCredentialsException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    // 로그아웃
    /*@PostMapping("/api/v1/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String usernmae = authentication.getPrincipal().toString();
        String role =  authentication.getAuthorities().iterator().next().getAuthority();
        TokenUserType userType = role.equals("ADMIN") ? TokenUserType.ADMIN : TokenUserType.GUEST;

    }*/


    // 토큰 검증
    @GetMapping("/api/v1/token-validation")
    public ResponseEntity<String> adminLoginConfirmation(HttpServletRequest request) {
        String accessToken = request.getHeader(HttpHeaders.AUTHORIZATION).split(" ")[1];
        try {
            if (authService.accessTokenIsExpired(accessToken)) {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                String authenticatedUsername = authentication.getPrincipal().toString();
                String role = authentication.getAuthorities().iterator().next().getAuthority();
                TokenUserType userType = role.equals("ADMIN") ? TokenUserType.ADMIN : TokenUserType.GUEST;
                String newAccessToken = authService.reissuingAccessToken(authenticatedUsername, userType);
                return ResponseEntity.status(HttpStatus.CREATED).body(newAccessToken);
            } else {
                return ResponseEntity.ok().body("인증 완료");
            }
        } catch (ExpiredTokenException | NoResultException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage());
        }
    }

}
