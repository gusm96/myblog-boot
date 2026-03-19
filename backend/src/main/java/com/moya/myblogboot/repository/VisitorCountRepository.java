package com.moya.myblogboot.repository;


import com.moya.myblogboot.domain.visitor.VisitorCount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;


@Repository
public interface VisitorCountRepository extends JpaRepository<VisitorCount, LocalDate> {

    Optional<VisitorCount> findByDate(LocalDate date);

    Optional<VisitorCount> findFirstByOrderByDateDesc();
}
