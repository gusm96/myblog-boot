package com.moya.myblogboot.service;

import com.moya.myblogboot.domain.admin.Admin;
import com.moya.myblogboot.domain.admin.AdminReqDto;
import com.moya.myblogboot.repository.AdminRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
public class LoginServiceTest {

    @Autowired
    LoginService loginService;
    @Autowired
    AdminRepository adminRepository;


    @DisplayName("로그인 기능")
    @Test
    void 관리자_로그인() {
        // given
        // Client로 부터 전달받을 데이터
        AdminReqDto req = new AdminReqDto("test", "password");
        // when

        // then
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            try{
                Admin admin = adminRepository.findById(req.getUsername()).orElseThrow(() ->
                        new EmptyResultDataAccessException("Admin not found with admin_name: " + req.getUsername(), 1));
            }catch (EmptyResultDataAccessException e){
                throw new IllegalArgumentException("아이디 또는 비밀번호를 확인하세요.", e);
            }
        });
    }
}
