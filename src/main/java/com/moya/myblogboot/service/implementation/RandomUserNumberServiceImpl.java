package com.moya.myblogboot.service.implementation;

import com.moya.myblogboot.dto.member.RandomUserNumberDto;
import com.moya.myblogboot.repository.RandomUserNumberRedisRepository;
import com.moya.myblogboot.service.RandomUserNumberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class RandomUserNumberServiceImpl implements RandomUserNumberService {

    private final RandomUserNumberRedisRepository randomUserNumberRedisRepository;

    @Override
    public RandomUserNumberDto getRandomUserNumber() {
        return generateUniqueRandomUserNumber();
    }

    private RandomUserNumberDto generateUniqueRandomUserNumber() {
        long randomUserNumber;
        // 중복되지 않는 번호가 나올때 까지 생성 및 검증을 반복
        do {
            randomUserNumber = ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE);
        } while (isRandomUserNumberInRedis(randomUserNumber));
        // TTL 생성
        long expireTime = calculateTTL();
        // Redis Store에 생성된 번호를 저장.
        saveRandomUserNumber(randomUserNumber, expireTime);
        return RandomUserNumberDto.builder()
                .number(randomUserNumber)
                .expireTime(expireTime)
                .build();
    }

    private long calculateTTL() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime midnight = now.plusDays(1).truncatedTo(ChronoUnit.DAYS);
        return ChronoUnit.SECONDS.between(now, midnight);
    }

    @Override
    public boolean isRandomUserNumberInRedis(long randomUserNumber) {
        return randomUserNumberRedisRepository.isExists(randomUserNumber);
    }

    private void saveRandomUserNumber(long number, long expireTime) {
        try {
            randomUserNumberRedisRepository.save(number, expireTime);
        } catch (Exception e) {
            log.error("임시번호 저장 실패: {}", e.getMessage());
        }
    }
}
