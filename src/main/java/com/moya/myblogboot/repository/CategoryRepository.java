package com.moya.myblogboot.repository;

import com.moya.myblogboot.domain.Category;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CategoryRepository implements CategoryRepositoryInf {

    private final EntityManager em;
    @Override
    public Long create(Category category) {
        em.persist(category);
        return category.getId();
    }

    @Override
    public Optional<Category> findOne(String categoryName) {
        Category category = em.createQuery("select c from Category c where c.name=:name", Category.class)
                .setParameter("name", categoryName)
                .getSingleResult();

        return Optional.ofNullable(category);
    }


    @Override
    public List<Category> categories() {
        List<Category> categories = em.createQuery("select c from Category c", Category.class).getResultList();
        return categories;
    }
}
