package com.moya.myblogboot.controller;


import com.moya.myblogboot.domain.guest.GuestReqDto;
import com.moya.myblogboot.service.GuestService;
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

    @PostMapping("/api/v1/guest")
    public ResponseEntity<String> joinGuest(@RequestBody @Valid GuestReqDto guestReqDto) {
        try{
        return ResponseEntity.ok().body(guestService.join(guestReqDto));}
        catch (DataIntegrityViolationException e){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 존재하는 아이디입니다.");
        }
    }

    @PostMapping("/api/v1/login/guest")
    public ResponseEntity<String> loginGuest(@RequestBody @Valid GuestReqDto guestReqDto) {
        try {
            String username = guestService.login(guestReqDto);
            return ResponseEntity.ok().body(username);
        } catch (NoResultException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 아이디입니다.");
        } catch (BadCredentialsException e){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "비밀번호가 일치하지 않습니다.");
        }
    }


}
