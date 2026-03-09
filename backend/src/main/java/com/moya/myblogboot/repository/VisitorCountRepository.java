package com.moya.myblogboot.repository;


import com.moya.myblogboot.domain.visitor.VisitorCount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface VisitorCountRepository extends JpaRepository<VisitorCount, Long>, VisitorCountCustomRepository {
}
