package com.moya.myblogboot.controller;


import com.moya.myblogboot.domain.PasswordStrength;
import com.moya.myblogboot.domain.guest.GuestReqDto;
import com.moya.myblogboot.service.GuestService;
import com.moya.myblogboot.service.AuthService;
import com.moya.myblogboot.service.PasswordStrengthCheck;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
public class GuestController {

    private final GuestService guestService;
    private final AuthService authService;
    private final PasswordStrengthCheck passwordStrengthCheck;


    @PostMapping("/api/v1/guest/password-validator")
    public ResponseEntity<PasswordStrength> passwordStrengthResponse(@RequestBody @Valid String password){
        return ResponseEntity.ok().body(passwordStrengthCheck.strengthCheck(password));
    }
    @PostMapping("/api/v1/guest/username-validator")
    public ResponseEntity<Boolean> validateUsername(@RequestBody @Valid String username){
        return ResponseEntity.ok().body(guestService.isValidUsername(username));
    }
    @PostMapping("/api/v1/guest")
    public ResponseEntity<String> joinGuest(@RequestBody @Valid GuestReqDto guestReqDto) {
        try{
            return ResponseEntity.ok().body(guestService.registerGuest(guestReqDto));
        }
        catch (IllegalStateException e){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
        catch (DataIntegrityViolationException e){
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
    }



}
