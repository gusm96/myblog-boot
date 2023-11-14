package com.moya.myblogboot.service;

import com.moya.myblogboot.domain.member.PasswordStrength;
import org.springframework.stereotype.Service;

@Service
public class PasswordStrengthCheck {

    public PasswordStrength strengthCheck(String password) {
        // 비밀번호 길이 검사
        passwordLengthCheck(password);
        return getPasswordStrength(password);
    }

    private PasswordStrength getPasswordStrength(String password) {
        int count = passwordStrengthCount(password);
        if (count == 2) {
            return PasswordStrength.RISK;
        } else if (count == 3) {
            return PasswordStrength.SAFE;
        } else if (count == 4) {
            return PasswordStrength.VERY_SAFE;
        }else {
            return PasswordStrength.HIGH_RISK;
        }
    }

    // 비밀번호 길이 체크
    private void passwordLengthCheck(String password) {
        if(password.length() < 8 || password.length() > 16 || password.isEmpty() ){
            throw new IllegalStateException("비밀번호는 8자이상 16자이하의 대/소문자, 숫자, 특수문자를 사용하여 입력하세요.");
        }
    }

    private int passwordStrengthCount(String password){
         int count = 0;
        if (passwordWithLowerCase(password)) {
            count++;
        }
        if (passwordWithUpperCase(password)){
            count ++;
        }
        if (passwordWithNumber(password)){
            count ++;
        }
        if (passwordWithSpecialSymbols(password)){
            count ++;
        }
        return count;
    }

    // 소문자 체크
    private boolean passwordWithUpperCase(String password){
        for(char c : password.toCharArray()){
            if(Character.isUpperCase(c)){
                return true;
            }
        }
        return false;
    }

    // 대문자 체크
    private boolean passwordWithLowerCase(String password) {
        for(char c : password.toCharArray()){
            if (Character.isLowerCase(c)) {
                return true;
            }
        }
        return false;
    }

    // 숫자 체크
    private boolean passwordWithNumber(String password){
        for(char c : password.toCharArray()){
            int n = c - '0';
            if (n >= 0 && n <= 9) {
                return true;
            }
        }
        return false;
    }
    // 특수문자 체크 "!@#$%^&*"
    private boolean passwordWithSpecialSymbols(String password){
        return !password.matches("[0-9|a-z|A-Z|ㄱ-ㅎ|ㅏ-ㅣ|가-힝|(|)|.|-]*");
    }
}
