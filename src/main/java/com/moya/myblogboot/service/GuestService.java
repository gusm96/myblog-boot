package com.moya.myblogboot.service;

import com.moya.myblogboot.domain.guest.Guest;
import com.moya.myblogboot.domain.guest.GuestReqDto;
import com.moya.myblogboot.repository.GuestRepository;
import jakarta.persistence.NoResultException;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class GuestService {

    private final GuestRepository guestRepository;
    private final PasswordEncoder passwordEncoder;


    public String join(GuestReqDto guestReqDto) {
        // 아이디 유효성 검사
        if (isInvalidData(guestReqDto.getUsername()))
            throw new DataIntegrityViolationException("중복된 회원입니다.");
        // 비밀번호 암호화
        String encodedPw = passwordEncoder.encode(guestReqDto.getPassword());
        guestReqDto.passwordEncode(encodedPw);

        guestRepository.save(guestReqDto.toEntity());
        return "등록에 성공하였습니다.";
    }
    // 아이디 유효성 검사
    private boolean isInvalidData(String username){
        return guestRepository.findByName(username).isPresent();
    }

}
