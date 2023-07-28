package com.moya.myblogboot.service;

import com.moya.myblogboot.domain.guest.Guest;
import com.moya.myblogboot.domain.guest.GuestReqDto;
import com.moya.myblogboot.repository.GuestRepository;
import jakarta.persistence.NoResultException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GuestService {

    private final GuestRepository guestRepository;
    private final PasswordEncoder passwordEncoder;

    public String join(GuestReqDto guestReqDto) {

        // 비밀번호 암호화
        String encodedPw = passwordEncoder.encode(guestReqDto.getPassword());
        guestReqDto.passwordEncode(encodedPw);

        try {
            guestRepository.save(guestReqDto.toEntity());
            return "등록에 성공하였습니다.";
        } catch (Exception e) {
            throw new IllegalArgumentException("등록에 실패하였습니다.");
        }
    }

    public String login(GuestReqDto guestReqDto) {
        Guest findGuest = guestRepository.findByName(guestReqDto.getUsername()).orElseThrow(() ->
                new NoResultException("존재하지 않는 아이디 입니다."));
        if (passwordEncoder.matches(guestReqDto.getPassword(), findGuest.getPassword())) {
            return findGuest.getUsername();
        }else {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }
    }
}
