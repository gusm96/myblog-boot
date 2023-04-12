package com.moya.myblogboot.service;

import com.moya.myblogboot.domain.Admin;
import com.moya.myblogboot.repository.AdminRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@SpringBootTest
@Transactional
public class LoginServiceTest {

    @Autowired
    LoginService loginService;
    @Autowired
    AdminRepository adminRepository;

    @Test
    void login(){
        // given
        String id = "moya";
        String pw = "moya134353@@";

        // when
        Optional<Admin> result = loginService.findById(id);

        // then
        if(pw.equals(result.get().getPw())){
            System.out.println("로그인 성공");
        }else{
            System.out.println("로그인 실패");
        }
    }
}
