package com.moya.myblogboot.service;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
public class GuestServiceTest {
    @Autowired
    private GuestRepository repository;

    @DisplayName("아이디 중복 테스트")
    @Test
    void  아이디_중복 () {
        // given
        String username = "moya";
        // when
        boolean result = repository.findByName(username).isPresent();
        // then
        Assertions.assertThat(result).isFalse();
    }
}
