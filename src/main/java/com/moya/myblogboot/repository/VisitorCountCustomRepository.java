package com.moya.myblogboot.repository;

import com.moya.myblogboot.domain.visitor.VisitorCount;

import java.time.LocalDate;
import java.util.Optional;

public interface VisitorCountCustomRepository {
    Optional<VisitorCount> findByDate(LocalDate date);
}
