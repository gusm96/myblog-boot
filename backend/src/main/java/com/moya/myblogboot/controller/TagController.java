package com.moya.myblogboot.controller;

import com.moya.myblogboot.dto.tag.TagCreateReqDto;
import com.moya.myblogboot.dto.tag.TagMergeReqDto;
import com.moya.myblogboot.dto.tag.TagRenameReqDto;
import com.moya.myblogboot.dto.tag.TagResDto;
import com.moya.myblogboot.service.TagLookupResult;
import com.moya.myblogboot.service.TagService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    @GetMapping("/api/v1/tags")
    public ResponseEntity<List<TagResDto>> getAllForAdmin() {
        return ResponseEntity.ok(tagService.retrieveAll());
    }

    @GetMapping("/api/v2/tags")
    public ResponseEntity<List<TagResDto>> getPublicTags() {
        return ResponseEntity.ok(tagService.retrievePublic());
    }

    @GetMapping("/api/v2/tags/popular")
    public ResponseEntity<List<TagResDto>> getPopularTags(@RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(tagService.retrievePopular(limit));
    }

    @GetMapping("/api/v2/tags/{slug}")
    public ResponseEntity<TagResDto> getTag(@PathVariable String slug) {
        TagLookupResult result = tagService.lookupBySlug(slug);
        if (result instanceof TagLookupResult.Redirect redirect) {
            URI location = UriComponentsBuilder.fromPath("/api/v2/tags/{slug}")
                    .buildAndExpand(redirect.targetSlug())
                    .toUri();
            return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY).location(location).build();
        }
        return ResponseEntity.ok(((TagLookupResult.Direct) result).tag());
    }

    @PostMapping("/api/v1/tags")
    public ResponseEntity<TagResDto> create(@RequestBody @Valid TagCreateReqDto request) {
        TagResDto created = tagService.create(request.name());
        URI location = UriComponentsBuilder.fromPath("/api/v2/tags/{slug}")
                .buildAndExpand(created.getSlug())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/api/v1/tags/{tagId}")
    public ResponseEntity<Void> rename(@PathVariable Long tagId, @RequestBody @Valid TagRenameReqDto request) {
        tagService.rename(tagId, request.name());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/api/v1/tags/{tagId}")
    public ResponseEntity<Void> delete(@PathVariable Long tagId) {
        tagService.delete(tagId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/v1/tags/merge")
    public ResponseEntity<Void> merge(@RequestBody @Valid TagMergeReqDto request) {
        tagService.merge(request.srcId(), request.dstId());
        return ResponseEntity.ok().build();
    }
}
