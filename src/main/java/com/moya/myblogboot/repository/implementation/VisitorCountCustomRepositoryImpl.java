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
        log.info("5");
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
}
