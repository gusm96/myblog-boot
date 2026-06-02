package com.moya.myblogboot.controller;

import com.moya.myblogboot.domain.post.SearchType;
import com.moya.myblogboot.dto.post.PostDetailResDto;
import com.moya.myblogboot.dto.post.PostListResDto;
import com.moya.myblogboot.dto.post.PostReqDto;
import com.moya.myblogboot.dto.post.PostSlugDto;
import com.moya.myblogboot.service.PostService;
import com.moya.myblogboot.service.PostViewCookieService;
import com.moya.myblogboot.utils.CookieUtil;
import com.moya.myblogboot.utils.PrincipalUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.security.Principal;
import java.util.List;

import static com.moya.myblogboot.constants.CookieName.*;

@Slf4j
@RestController
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final PostViewCookieService postViewCookieService;

    // Next.js sitemap.ts + generateStaticParams() 전용 (content 제외 경량 응답)
    @GetMapping("/api/v1/posts/slugs")
    public ResponseEntity<List<PostSlugDto>> getAllSlugs() {
        return ResponseEntity.ok()
                .header("Cache-Control", "public, max-age=3600")
                .body(postService.getAllSlugs());
    }

    @GetMapping("/api/v1/posts")
    public ResponseEntity<PostListResDto> getAllPosts(@RequestParam(name = "p", defaultValue = "1") int page) {
        return ResponseEntity.ok()
                .header("Cache-Control", "public, max-age=60")
                .body(postService.retrieveAll(getPage(page)));
    }

    @GetMapping("/api/v1/posts/category")
    public ResponseEntity<PostListResDto> getCategoryPosts(
            @RequestParam("c") String category,
            @RequestParam(name = "p", defaultValue = "1") int page
    ) {
        return ResponseEntity.ok()
                .header("Cache-Control", "public, max-age=60")
                .body(postService.retrieveAllByCategory(category, getPage(page)));
    }

    @GetMapping("/api/v1/posts/search")
    public ResponseEntity<PostListResDto> getSearchedPosts(
            @RequestParam("type") SearchType searchType,
            @RequestParam("contents") String searchContents,
            @RequestParam(name = "p", defaultValue = "1") int page) {
        return ResponseEntity.ok().body(postService.retrieveAllBySearched(searchType, searchContents, getPage(page)));
    }

    // slug 또는 숫자 ID 모두 처리 — HMAC 쿠키로 중복 조회 방지
    @GetMapping("/api/v1/posts/{identifier}")
    public ResponseEntity<PostDetailResDto> getPostDetail(@PathVariable("identifier") String identifier) {
        PostDetailResDto dto = postService.getPublicPostDetail(identifier);
        return ResponseEntity.ok()
                .header("Cache-Control", "public, max-age=60")
                .body(dto);
    }

    @PostMapping("/api/v1/posts/{postId}/views")
    public ResponseEntity<Long> incrementPostViews(@PathVariable("postId") Long postId,
                                                   HttpServletRequest request,
                                                   HttpServletResponse response) {
        Cookie cookie = CookieUtil.findCookie(request, VIEWED_POSTS);
        String cookieValue = (cookie != null) ? cookie.getValue() : null;
        boolean valid = postViewCookieService.isValid(cookieValue);

        Long totalViews;
        if (!valid || !postViewCookieService.isViewed(cookieValue, postId)) {
            totalViews = postService.incrementPublicPostViews(postId);
            String newValue = postViewCookieService.addViewed(valid ? cookieValue : null, postId);
            response.addCookie(CookieUtil.addCookie(VIEWED_POSTS, newValue, postViewCookieService.secondsUntilMidnight()));
        } else {
            totalViews = postService.getPublicPostViews(postId);
        }
        return ResponseEntity.ok(totalViews);
    }

    // v8 → v1 301 redirect (하위 호환)
    @GetMapping("/api/v8/posts/{postId}")
    public ResponseEntity<Void> getPostDetailV8Redirect(@PathVariable("postId") Long postId) {
        URI redirectUri = UriComponentsBuilder.fromPath("/api/v1/posts/{postId}")
                .buildAndExpand(postId)
                .toUri();
        return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY).location(redirectUri).build();
    }

    @GetMapping("/api/v1/management/posts/{postId}")
    public ResponseEntity<PostDetailResDto> getPostDetailForAdmin(@PathVariable("postId") Long postId,
                                                                  Principal principal) {
        Long memberId = PrincipalUtil.getMemberId(principal);
        log.info("MemberId = {}", memberId);
        return ResponseEntity.ok().body(postService.getPostDetail(postId));
    }

    @PostMapping("/api/v1/posts")
    public ResponseEntity<Long> writePost(@RequestBody @Valid PostReqDto postReqDto,
                                          Principal principal) {
        Long memberId = PrincipalUtil.getMemberId(principal);
        return ResponseEntity.ok().body(postService.write(postReqDto, memberId));
    }

    @PutMapping("/api/v1/posts/{postId}")
    public ResponseEntity<Long> editPost(@PathVariable("postId") Long postId,
                                         @RequestBody @Valid PostReqDto postReqDto,
                                         Principal principal) {
        Long memberId = PrincipalUtil.getMemberId(principal);
        return ResponseEntity.ok().body(postService.edit(memberId, postId, postReqDto));
    }

    @DeleteMapping("/api/v1/posts/{postId}")
    public ResponseEntity<Boolean> deletePost(@PathVariable("postId") Long postId,
                                              Principal principal) {
        Long memberId = PrincipalUtil.getMemberId(principal);
        postService.delete(postId, memberId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    private int getPage(int page) {
        return page - 1;
    }
}
