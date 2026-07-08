package com.moya.myblogboot.service.implementation;

import com.moya.myblogboot.AbstractContainerBaseTest;
import com.moya.myblogboot.domain.tag.Tag;
import com.moya.myblogboot.domain.tag.TagAlias;
import com.moya.myblogboot.dto.tag.TagResDto;
import com.moya.myblogboot.exception.BusinessException;
import com.moya.myblogboot.exception.ErrorCode;
import com.moya.myblogboot.exception.custom.DuplicateException;
import com.moya.myblogboot.repository.TagAliasRepository;
import com.moya.myblogboot.repository.TagRepository;
import com.moya.myblogboot.service.TagService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
@SpringBootTest
@ActiveProfiles("test")
class TagServiceImplTest extends AbstractContainerBaseTest {

    @Autowired private TagService tagService;
    @Autowired private TagRepository tagRepository;
    @Autowired private TagAliasRepository tagAliasRepository;

    @Test
    @DisplayName("create creates a single admin tag without per-post count validation coupling")
    void create_createsSingleTag() {
        TagResDto created = tagService.create("Spring Boot");

        assertThat(created.getSlug()).isEqualTo("spring-boot");
        assertThat(tagRepository.findBySlug("spring-boot")).isPresent();
    }

    @Test
    @DisplayName("create rejects duplicate active tag")
    void create_duplicateActiveTag() {
        tagRepository.save(Tag.builder().name("Spring").slug("spring").build());

        assertThatThrownBy(() -> tagService.create("Spring"))
                .isInstanceOf(DuplicateException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DUPLICATE_TAG);
    }

    @Test
    @DisplayName("resolveOrCreate deduplicates normalized input while preserving order")
    void resolveOrCreate_deduplicatesBySlug() {
        List<Tag> tags = tagService.resolveOrCreate(List.of("Spring", "spring", "JPA"));

        assertThat(tags).extracting(Tag::getSlug).containsExactly("spring", "jpa");
    }

    @Test
    @DisplayName("merge repoints inbound aliases and preserves source slug as alias")
    void merge_repointsAliases() {
        Tag src = tagRepository.save(Tag.builder().name("Spring").slug("spring").build());
        Tag dst = tagRepository.save(Tag.builder().name("JPA").slug("jpa").build());
        tagAliasRepository.save(TagAlias.builder().fromSlug("old-spring").toTag(src).build());

        tagService.merge(src.getId(), dst.getId());

        assertThat(tagRepository.findById(src.getId())).isEmpty();
        assertThat(tagAliasRepository.findByFromSlug("old-spring").orElseThrow().getToTag().getId())
                .isEqualTo(dst.getId());
        assertThat(tagAliasRepository.findByFromSlug("spring").orElseThrow().getToTag().getId())
                .isEqualTo(dst.getId());
    }

    @Test
    @DisplayName("merge rejects damaged alias cycle")
    void merge_rejectsAliasCycle() {
        Tag src = tagRepository.save(Tag.builder().name("Spring").slug("spring").build());
        Tag dst = tagRepository.save(Tag.builder().name("JPA").slug("jpa").build());
        tagAliasRepository.save(TagAlias.builder().fromSlug(dst.getSlug()).toTag(src).build());

        assertThatThrownBy(() -> tagService.merge(src.getId(), dst.getId()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.TAG_MERGE_CYCLE);
    }
}
