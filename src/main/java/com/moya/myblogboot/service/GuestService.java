package com.moya.myblogboot.service;

import com.moya.myblogboot.domain.guest.GuestReqDto;
import com.moya.myblogboot.repository.GuestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GuestService {

    private final GuestRepository guestRepository;
    private final PasswordEncoder passwordEncoder;

    // 아이디 정규식
    private static final String USERNAME_REGEX = "^[a-z0-9]{3,15}$";
    // 비밀번호 정규식
    private static final String PASSWORD_REGEX = "^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[!@#$%^&*]).{8,16}$";

    @Transactional
    public String registerGuest (GuestReqDto guestReqDto) {
        if(!validateUsernameFormat(guestReqDto.getUsername()))
            throw new IllegalStateException("아이디는 3글자 이상 15글자 이하의 (소)문자와 숫자로 작성하세요.");
        if (isValidUsername(guestReqDto.getUsername()))
            throw new DataIntegrityViolationException("중복된 회원입니다.");
        if (!validatePasswordFormat(guestReqDto.getPassword()))
            throw new IllegalStateException("비밀번호는 8자 이상 16자 이하의 대/소문자, 숫자, 특수문자(!@#$%^&*)를 사용하여 입력하세요");

        // 비밀번호 암호화
        String encodedPw = passwordEncoder.encode(guestReqDto.getPassword());
        guestReqDto.passwordEncode(encodedPw);

        guestRepository.save(guestReqDto.toEntity());
        return "등록에 성공하였습니다.";
    }

    // 아이디 정규식 검증
    private boolean validateUsernameFormat(String username) {
        return username.matches(USERNAME_REGEX);
    }
    // 비밀번호 정규식 검증
    private boolean validatePasswordFormat(String password){
        return password.matches(PASSWORD_REGEX);
    }
    // 아이디 유효성 검사
    public boolean isValidUsername(String username){
        return guestRepository.findByName(username).isPresent();
    }

}
