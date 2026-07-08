package com.moya.myblogboot.service.implementation;

import com.moya.myblogboot.configuration.TagProperties;
import com.moya.myblogboot.domain.event.TagChangeEvent;
import com.moya.myblogboot.domain.tag.PostTag;
import com.moya.myblogboot.domain.tag.Tag;
import com.moya.myblogboot.domain.tag.TagAlias;
import com.moya.myblogboot.dto.tag.TagResDto;
import com.moya.myblogboot.exception.BusinessException;
import com.moya.myblogboot.exception.ErrorCode;
import com.moya.myblogboot.exception.custom.DuplicateException;
import com.moya.myblogboot.exception.custom.EntityNotFoundException;
import com.moya.myblogboot.repository.PostTagRepository;
import com.moya.myblogboot.repository.TagAliasRepository;
import com.moya.myblogboot.repository.TagRepository;
import com.moya.myblogboot.service.TagLookupResult;
import com.moya.myblogboot.service.TagService;
import com.moya.myblogboot.utils.TagNormalizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TagServiceImpl implements TagService {

    private final TagRepository tagRepository;
    private final TagAliasRepository tagAliasRepository;
    private final PostTagRepository postTagRepository;
    private final TagProperties tagProperties;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public List<Tag> resolveOrCreate(List<String> rawNames) {
        validateCount(rawNames);
        Map<String, String> slugToName = new LinkedHashMap<>();
        for (String rawName : rawNames) {
            String name = TagNormalizer.normalizeName(rawName);
            String slug = TagNormalizer.toSlug(name);
            slugToName.putIfAbsent(slug, name);
        }
        validateCount(slugToName.values().stream().toList());
        return slugToName.entrySet().stream()
                .map(entry -> tagRepository.findBySlug(entry.getKey())
                        .orElseGet(() -> createInternal(entry.getValue(), entry.getKey())))
                .toList();
    }

    @Override
    @Transactional
    public TagResDto create(String rawName) {
        String name = TagNormalizer.normalizeName(rawName);
        String slug = TagNormalizer.toSlug(name);
        if (tagRepository.existsBySlug(slug) || tagAliasRepository.existsById(slug)) {
            throw new DuplicateException(ErrorCode.DUPLICATE_TAG);
        }
        return TagResDto.of(createInternal(name, slug));
    }

    @Override
    public List<TagResDto> retrieveAll() {
        return tagRepository.findAll().stream().map(TagResDto::of).toList();
    }

    @Override
    public List<TagResDto> retrievePublic() {
        return tagRepository.findAllByPostCountGreaterThanOrderByPostCountDesc(0)
                .stream()
                .map(TagResDto::of)
                .toList();
    }

    @Override
    public List<TagResDto> retrievePopular(int limit) {
        return tagRepository.findByPostCountGreaterThanOrderByPostCountDesc(0, PageRequest.of(0, limit))
                .stream()
                .map(TagResDto::of)
                .toList();
    }

    @Override
    public TagLookupResult lookupBySlug(String slug) {
        return tagRepository.findBySlug(slug)
                .<TagLookupResult>map(tag -> new TagLookupResult.Direct(TagResDto.of(tag)))
                .orElseGet(() -> tagAliasRepository.findByFromSlug(slug)
                        .map(alias -> new TagLookupResult.Redirect(alias.getToTag().getSlug()))
                        .orElseThrow(() -> new EntityNotFoundException(ErrorCode.TAG_NOT_FOUND)));
    }

    @Override
    @Transactional
    public void merge(Long srcTagId, Long dstTagId) {
        if (srcTagId.equals(dstTagId)) {
            throw new BusinessException(ErrorCode.TAG_MERGE_SAME_TARGET);
        }
        Tag src = findTag(srcTagId);
        Tag dst = findTag(dstTagId);
        String srcSlug = src.getSlug();
        String dstSlug = dst.getSlug();
        if (tagAliasRepository.findByFromSlug(dstSlug)
                .map(alias -> alias.getToTag().getId().equals(srcTagId))
                .orElse(false)) {
            throw new BusinessException(ErrorCode.TAG_MERGE_CYCLE);
        }
        List<PostTag> srcPostTags = List.copyOf(postTagRepository.findAllByTagId(srcTagId));

        for (PostTag srcPostTag : srcPostTags) {
            boolean alreadyHasDst = postTagRepository.existsByPostIdAndTagId(srcPostTag.getPost().getId(), dstTagId);
            postTagRepository.delete(srcPostTag);
            if (srcPostTag.getPost().getDeleteDate() == null) {
                src.decrementPostCount();
            }
            if (!alreadyHasDst) {
                postTagRepository.save(PostTag.builder()
                        .post(srcPostTag.getPost())
                        .tag(dst)
                        .sortOrder(srcPostTag.getSortOrder())
                        .build());
                if (srcPostTag.getPost().getDeleteDate() == null) {
                    dst.incrementPostCount();
                }
            }
        }

        long expectedInboundAliases = tagAliasRepository.countByToTagId(srcTagId);
        int affectedInboundAliases = tagAliasRepository.repointTo(src, dst);
        if (affectedInboundAliases != expectedInboundAliases) {
            log.warn("Tag alias repoint affected row mismatch: srcTagId={}, dstTagId={}, expected={}, affected={}",
                    srcTagId, dstTagId, expectedInboundAliases, affectedInboundAliases);
        }
        Tag srcRef = tagRepository.getReferenceById(srcTagId);
        Tag dstRef = tagRepository.getReferenceById(dstTagId);
        tagAliasRepository.save(TagAlias.builder().fromSlug(srcSlug).toTag(dstRef).build());
        tagRepository.delete(srcRef);
        eventPublisher.publishEvent(new TagChangeEvent(this, "MERGED", dstTagId, List.of(srcSlug, dstSlug)));
    }

    @Override
    @Transactional
    public void rename(Long tagId, String newName) {
        Tag tag = findTag(tagId);
        tag.rename(TagNormalizer.normalizeName(newName));
        eventPublisher.publishEvent(new TagChangeEvent(this, "RENAMED", tagId, List.of(tag.getSlug())));
    }

    @Override
    @Transactional
    public void delete(Long tagId) {
        Tag tag = findTag(tagId);
        long totalPostTags = postTagRepository.countByTagId(tagId);
        if (totalPostTags > 0) {
            long activePostTags = postTagRepository.countByTagIdAndPostNotDeleted(tagId);
            throw new BusinessException(activePostTags > 0
                    ? ErrorCode.TAG_HAS_POSTS
                    : ErrorCode.TAG_HAS_SOFT_DELETED_POSTS);
        }
        if (tagAliasRepository.countByToTagId(tagId) > 0) {
            throw new BusinessException(ErrorCode.TAG_HAS_INBOUND_ALIASES);
        }
        String slug = tag.getSlug();
        tagRepository.delete(tag);
        eventPublisher.publishEvent(new TagChangeEvent(this, "DELETED", tagId, List.of(slug)));
    }

    private Tag createInternal(String name, String slug) {
        if (tagAliasRepository.existsById(slug)) {
            throw new DuplicateException(ErrorCode.DUPLICATE_TAG);
        }
        Tag tag = tagRepository.save(Tag.builder().name(name).slug(slug).build());
        eventPublisher.publishEvent(new TagChangeEvent(this, "CREATED", tag.getId(), List.of(tag.getSlug())));
        return tag;
    }

    private Tag findTag(Long tagId) {
        return tagRepository.findById(tagId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.TAG_NOT_FOUND));
    }

    private void validateCount(List<?> tags) {
        if (tags == null || tags.size() < tagProperties.minPerPost()) {
            throw new BusinessException(ErrorCode.TAG_COUNT_BELOW_MIN);
        }
        if (tags.size() > tagProperties.maxPerPost()) {
            throw new BusinessException(ErrorCode.TAG_COUNT_ABOVE_MAX);
        }
    }
}
