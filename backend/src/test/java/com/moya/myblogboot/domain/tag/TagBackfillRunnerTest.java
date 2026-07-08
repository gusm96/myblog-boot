package com.moya.myblogboot.domain.tag;

import com.moya.myblogboot.AbstractContainerBaseTest;
import com.moya.myblogboot.repository.TagRepository;
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
class TagBackfillRunnerTest extends AbstractContainerBaseTest {

    @Autowired private EntityManager entityManager;
    @Autowired private TagRepository tagRepository;

    @Test
    @DisplayName("run skips when category table is missing")
    void run_skipsWhenCategoryTableMissing() {
        TagBackfillRunner runner = new TagBackfillRunner(entityManager, tagRepository);

        runner.run();

        assertThat(tagRepository.count()).isZero();
    }

    @Test
    @DisplayName("run skips when tags already exist")
    void run_skipsWhenTagsAlreadyPopulated() {
        tagRepository.save(Tag.builder().name("Seed").slug("seed").build());
        TagBackfillRunner runner = new TagBackfillRunner(entityManager, tagRepository);

        runner.run();

        assertThat(tagRepository.count()).isOne();
    }
}
