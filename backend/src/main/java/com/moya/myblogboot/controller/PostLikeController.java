package com.moya.myblogboot.controller;

import com.moya.myblogboot.constants.CookieName;
import com.moya.myblogboot.exception.ErrorCode;
import com.moya.myblogboot.exception.custom.DuplicateException;
import com.moya.myblogboot.exception.custom.EntityNotFoundException;
import com.moya.myblogboot.service.PostLikeHmacService;
import com.moya.myblogboot.service.PostLikeService;
import com.moya.myblogboot.service.PostService;
import com.moya.myblogboot.utils.CookieUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class PostLikeController {

    private final PostLikeHmacService postLikeHmacService;
    private final PostLikeService postLikeService;
    private final PostService postService;

    @GetMapping("/api/v2/likes/{postId}")
    public ResponseEntity<Boolean> checkPostLike(
            @PathVariable("postId") Long postId,
            @CookieValue(name = CookieName.LIKED_POSTS, required = false) String likedCookie) {

        boolean liked = postLikeHmacService.isValid(likedCookie)
                && postLikeHmacService.isLiked(likedCookie, postId);
        return ResponseEntity.ok(liked);
    }

    @PostMapping("/api/v2/likes/{postId}")
    public ResponseEntity<Long> addPostLike(
            @PathVariable("postId") Long postId,
            @CookieValue(name = CookieName.LIKED_POSTS, required = false) String likedCookie,
            HttpServletResponse response) {

        if (postLikeHmacService.isValid(likedCookie)
                && postLikeHmacService.isLiked(likedCookie, postId)) {
            throw new DuplicateException(ErrorCode.DUPLICATE_POST_LIKE);
        }

        Long totalLikes = postLikeService.addLikes(postId);
        String newCookieValue = postLikeHmacService.addLike(likedCookie, postId);
        response.addCookie(CookieUtil.addCookie(
                CookieName.LIKED_POSTS, newCookieValue, 365 * 24 * 60 * 60));
        return ResponseEntity.ok(totalLikes);
    }

    @DeleteMapping("/api/v2/likes/{postId}")
    public ResponseEntity<Long> cancelPostLike(
            @PathVariable("postId") Long postId,
            @CookieValue(name = CookieName.LIKED_POSTS, required = false) String likedCookie,
            HttpServletResponse response) {

        if (!postLikeHmacService.isValid(likedCookie)
                || !postLikeHmacService.isLiked(likedCookie, postId)) {
            throw new EntityNotFoundException(ErrorCode.POST_LIKE_NOT_FOUND);
        }

        Long totalLikes = postLikeService.cancelLikes(postId);
        String newCookieValue = postLikeHmacService.removeLike(likedCookie, postId);

        if (newCookieValue == null) {
            response.addCookie(CookieUtil.addCookie(CookieName.LIKED_POSTS, "", 0));
        } else {
            response.addCookie(CookieUtil.addCookie(
                    CookieName.LIKED_POSTS, newCookieValue, 365 * 24 * 60 * 60));
        }
        return ResponseEntity.ok(totalLikes);
    }

    @GetMapping("/api/v1/posts/{postId}/views")
    public ResponseEntity<Long> getViews(@PathVariable("postId") Long postId) {
        return ResponseEntity.ok().body(postService.getPostDetail(postId).getViews());
    }

    @GetMapping("/api/v1/posts/{postId}/likes")
    public ResponseEntity<Long> getLikes(@PathVariable("postId") Long postId) {
        return ResponseEntity.ok().body(postService.getPostDetail(postId).getLikes());
    }
}
