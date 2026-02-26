package com.moya.myblogboot.repository.implementation;

import com.moya.myblogboot.domain.visitor.VisitorCount;
import com.moya.myblogboot.repository.VisitorCountCustomRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Slf4j
@Repository
@RequiredArgsConstructor
public class VisitorCountCustomRepositoryImpl implements VisitorCountCustomRepository {

    private final EntityManager em;

    @Override
    public Optional<VisitorCount> findByDate(LocalDate date) {
        try {
            VisitorCount visitorCount = em.createQuery("select v from VisitorCount v where v.date =: date", VisitorCount.class)
                    .setParameter("date", date)
                    .getSingleResult();

            return Optional.of(visitorCount);
        } catch (NoResultException e) {
            return Optional.empty();
        } catch (PersistenceException e) {
            log.error("Persistence error occurred: {}", e.getMessage());
            throw new RuntimeException("Database error", e);
        }
    }

    @Override
    public Optional<VisitorCount> findRecentVisitorCount() {
        try {
            VisitorCount visitorCount = em.createQuery("select v from VisitorCount v order by v.date desc", VisitorCount.class)
                    .setMaxResults(1) // 가장 최근 날짜의 값 하나만 가져온다.
                    .getSingleResult();
            return Optional.of(visitorCount);
        } catch (NoResultException e) {
            return Optional.empty();
        } catch (PersistenceException e) {
            throw new RuntimeException("Database error", e);
        }
    }
}
