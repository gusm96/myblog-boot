package com.moya.myblogboot.service.implementation;

import com.moya.myblogboot.repository.VisitorCountRedisRepository;
import com.moya.myblogboot.repository.VisitorCountRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@SpringBootTest
public class VisitorCountServiceV2ImplTest {

    @Autowired
    private VisitorCountRepository visitorCountRepository;
    @Autowired
    private VisitorCountRedisRepository visitorCountRedisRepository;

    @Test
    @DisplayName("Redis Store에서 VisitorCount 찾기")
    void retrieveVisitorCountFromRedisStore() {
        // given

        // when

        // then
    }

}
