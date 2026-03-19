package com.moya.myblogboot.repository;

import com.moya.myblogboot.dto.visitor.VisitorCountDto;

import java.util.Optional;


public interface VisitorCountRedisRepository {

    void save(String keyDate, VisitorCountDto visitorCountDto);

    Optional<VisitorCountDto> findByDate(String keyDate);

    Optional<VisitorCountDto> increment(String keyDate);

}
