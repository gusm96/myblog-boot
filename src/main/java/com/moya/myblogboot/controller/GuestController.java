package com.moya.myblogboot.controller;


import com.moya.myblogboot.domain.guest.GuestReqDto;
import com.moya.myblogboot.domain.token.Token;
import com.moya.myblogboot.service.GuestService;
import com.moya.myblogboot.service.LoginService;
import jakarta.persistence.NoResultException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
public class GuestController {

    private final GuestService guestService;
    private final LoginService loginService;

    @PostMapping("/api/v1/guest")
    public ResponseEntity<String> joinGuest(@RequestBody @Valid GuestReqDto guestReqDto) {
        try{
        return ResponseEntity.ok().body(guestService.join(guestReqDto));}
        catch (DataIntegrityViolationException e){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 존재하는 아이디입니다.");
        }
    }

    @PostMapping("/api/v1/login/guest")
    public ResponseEntity<Token> loginGuest(@RequestBody @Valid GuestReqDto guestReqDto) {
        try {
            Token token = loginService.guestLogin(guestReqDto);
            return ResponseEntity.ok().body(token);
        } catch (NoResultException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "아이디 또는 비밀번호를 확인하세요.");
        } catch (BadCredentialsException e){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "아이디 또는 비밀번호를 확인하세요.");
        }
    }


}
