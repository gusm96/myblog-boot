package com.moya.myblogboot.service;

import com.moya.myblogboot.domain.PasswordStrength;
import static org.junit.jupiter.api.Assertions.*;

import static org.assertj.core.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class PasswordStrengthTest {

    @Autowired
    private PasswordStrengthCheck passwordLengthCheck;
    // 요구사항
    // 8자 이상 16자 이하 대문자, 소문자, 숫자, 특수문자 사용
    // 공백 불가능
    // 문자
    // 숫자
    // 특수
    // 안전도 1단계 (대문자, 소문자, 숫자, 특수 중 2개만 ex = abcd123 ) 낮음
    // 안전도 2단계 ( '' 중 3개  ex = Abcd123) 적정
    // 안전도 3단계 (모두 해당 ex = Abcd123!@ ) 높음
    @DisplayName("안전도 1단계")
    @Test
    void 안전도 () {
        // given
        String password1 = "abcd1234"; // 조건 두가지 충족 LOW
        String password2 = "Abcd1234"; // 조건 세가지 충족 MID
        String password3 = "Abcd1234@@"; // 조건 네가지 모두 충족 HIGH
        String password4 = "abcdasdsasd"; // 조건 중 한가지만.. BAD
        // when
        PasswordStrength strength = passwordLengthCheck.strengthCheck(password1);
        
        // then
        assertThat(PasswordStrength.BAD).isEqualTo(strength);
    }

}
