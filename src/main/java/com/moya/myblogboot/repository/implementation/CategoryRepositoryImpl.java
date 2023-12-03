package com.moya.myblogboot.repository.implementation;

import com.moya.myblogboot.domain.category.Category;
import com.moya.myblogboot.repository.CategoryRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CategoryRepositoryImpl implements CategoryRepository {

    private final EntityManager em;
    @Override
    public void save(Category category) {
        em.persist(category);
    }

    @Override
    public Optional<Category> findById(Long categoryId) {
        try {
            Category category = em.find(Category.class, categoryId);
            return Optional.ofNullable(category);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<Category> findByName(String categoryName) {
        try {
            Category result = em.createQuery("select c from Category c where c.name =:categoryName", Category.class)
                    .setParameter("categoryName", categoryName)
                    .getSingleResult();
            return Optional.ofNullable(result);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    @Override
    public List<Category> categories() {
        List<Category> categories = em.createQuery("select c from Category c", Category.class).getResultList();
        return categories;
    }

    @Override
    public void removeCategory(Category category) {
        em.remove(category);
    }
}
