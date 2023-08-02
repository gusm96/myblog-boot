package com.moya.myblogboot.service;

import org.springframework.stereotype.Service;

@Service
public class UsernameValidator {
    private static final String USERNAME_REGEX = "^[a-z0-9]{3,15}$";

    public void isValidUsername(String username) {
        if(!username.matches(USERNAME_REGEX)){
            throw new IllegalStateException("아이디는 3글자 이상 15글자 이하의 (소)문자와 숫자로 작성하세요.");
        }
    }
}
