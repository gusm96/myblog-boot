package com.moya.myblogboot.repository;

import com.moya.myblogboot.dto.visitor.VisitorCountDto;

import java.util.Optional;


public interface VisitorCountRedisRepository {

    void save(String keyDate, VisitorCountDto visitorCountDto);

    Optional<VisitorCountDto> findByDate(String keyDate);

    VisitorCountDto increment(String keyDate);

}
