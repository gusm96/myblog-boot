package com.moya.myblogboot.controller;

import com.moya.myblogboot.domain.post.SearchType;
import com.moya.myblogboot.dto.post.PostDetailResDto;
import com.moya.myblogboot.dto.post.PostListResDto;
import com.moya.myblogboot.dto.post.PostReqDto;
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

import java.security.Principal;

import static com.moya.myblogboot.constants.CookieName.*;

@Slf4j
@RestController
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final PostViewCookieService postViewCookieService;

    @GetMapping("/api/v1/posts")
    public ResponseEntity<PostListResDto> getAllPosts(@RequestParam(name = "p", defaultValue = "1") int page) {
        return ResponseEntity.ok().body(postService.retrieveAll(getPage(page)));
    }

    @GetMapping("/api/v1/posts/category")
    public ResponseEntity<PostListResDto> getCategoryPosts(
            @RequestParam("c") String category,
            @RequestParam(name = "p", defaultValue = "1") int page
    ) {
        return ResponseEntity.ok().body(postService.retrieveAllByCategory(category, getPage(page)));
    }

    @GetMapping("/api/v1/posts/search")
    public ResponseEntity<PostListResDto> getSearchedPosts(
            @RequestParam("type") SearchType searchType,
            @RequestParam("contents") String searchContents,
            @RequestParam(name = "p", defaultValue = "1") int page) {
        return ResponseEntity.ok().body(postService.retrieveAllBySearched(searchType, searchContents, getPage(page)));
    }

    // HMAC 서명 쿠키로 중복 조회를 Stateless하게 방지 — 미조회 시 조회수 증가
    @GetMapping("/api/v8/posts/{postId}")
    public ResponseEntity<PostDetailResDto> getPostDetailV8(@PathVariable("postId") Long postId,
                                                            HttpServletRequest request,
                                                            HttpServletResponse response) {
        Cookie cookie = CookieUtil.findCookie(request, VIEWED_POSTS);
        String cookieValue = (cookie != null) ? cookie.getValue() : null;

        boolean valid = postViewCookieService.isValid(cookieValue);

        if (!valid || !postViewCookieService.isViewed(cookieValue, postId)) {
            PostDetailResDto dto = postService.getPostDetailAndIncrementViews(postId);
            String newValue = postViewCookieService.addViewed(valid ? cookieValue : null, postId);
            response.addCookie(CookieUtil.addCookie(VIEWED_POSTS, newValue, postViewCookieService.secondsUntilMidnight()));
            return ResponseEntity.ok(dto);
        }

        return ResponseEntity.ok(postService.getPostDetail(postId));
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
