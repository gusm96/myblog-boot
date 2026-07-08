package com.moya.myblogboot.repository;

import com.moya.myblogboot.AbstractContainerBaseTest;
import com.moya.myblogboot.domain.tag.Tag;
import com.moya.myblogboot.domain.tag.TagAlias;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
@SpringBootTest
@ActiveProfiles("test")
class TagAliasRepositoryTest extends AbstractContainerBaseTest {

    @Autowired private TagRepository tagRepository;
    @Autowired private TagAliasRepository tagAliasRepository;
    @Autowired private EntityManager entityManager;

    @Test
    @DisplayName("repointTo changes aliases to destination tag")
    void repointTo_changesToTag() {
        Tag src = tagRepository.save(Tag.builder().name("Spring").slug("spring").build());
        Tag dst = tagRepository.save(Tag.builder().name("JPA").slug("jpa").build());
        tagAliasRepository.save(TagAlias.builder().fromSlug("old-spring").toTag(src).build());
        entityManager.flush();
        entityManager.clear();

        int affected = tagAliasRepository.repointTo(src, dst);

        assertThat(affected).isOne();
        TagAlias alias = tagAliasRepository.findByFromSlug("old-spring").orElseThrow();
        assertThat(alias.getToTag().getId()).isEqualTo(dst.getId());
    }

    @Test
    @DisplayName("repointTo clears stale persistence context")
    void repointTo_clearsPersistenceContext() {
        Tag src = tagRepository.save(Tag.builder().name("Spring Boot").slug("spring-boot").build());
        Tag dst = tagRepository.save(Tag.builder().name("Hibernate").slug("hibernate").build());
        TagAlias loaded = tagAliasRepository.save(TagAlias.builder().fromSlug("boot").toTag(src).build());
        entityManager.flush();

        tagAliasRepository.repointTo(src, dst);

        TagAlias reloaded = tagAliasRepository.findByFromSlug(loaded.getFromSlug()).orElseThrow();
        assertThat(reloaded.getToTag().getId()).isEqualTo(dst.getId());
    }
}
