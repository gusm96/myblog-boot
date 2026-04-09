package com.moya.myblogboot.controller;

import com.moya.myblogboot.constants.CookieName;
import com.moya.myblogboot.exception.ErrorCode;
import com.moya.myblogboot.exception.custom.DuplicateException;
import com.moya.myblogboot.exception.custom.EntityNotFoundException;
import com.moya.myblogboot.service.BoardLikeHmacService;
import com.moya.myblogboot.service.BoardLikeService;
import com.moya.myblogboot.service.BoardService;
import com.moya.myblogboot.utils.CookieUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class BoardLikeController {

    private final BoardLikeHmacService boardLikeHmacService;
    private final BoardLikeService boardLikeService;
    private final BoardService boardService;

    @GetMapping("/api/v2/likes/{boardId}")
    public ResponseEntity<Boolean> checkBoardLike(
            @PathVariable("boardId") Long boardId,
            @CookieValue(name = CookieName.LIKED_BOARDS, required = false) String likedCookie) {

        boolean liked = boardLikeHmacService.isValid(likedCookie)
                && boardLikeHmacService.isLiked(likedCookie, boardId);
        return ResponseEntity.ok(liked);
    }

    @PostMapping("/api/v2/likes/{boardId}")
    public ResponseEntity<Long> addBoardLike(
            @PathVariable("boardId") Long boardId,
            @CookieValue(name = CookieName.LIKED_BOARDS, required = false) String likedCookie,
            HttpServletResponse response) {

        if (boardLikeHmacService.isValid(likedCookie)
                && boardLikeHmacService.isLiked(likedCookie, boardId)) {
            throw new DuplicateException(ErrorCode.DUPLICATE_BOARD_LIKE);
        }

        Long totalLikes = boardLikeService.addLikes(boardId);
        String newCookieValue = boardLikeHmacService.addLike(likedCookie, boardId);
        response.addCookie(CookieUtil.addCookie(
                CookieName.LIKED_BOARDS, newCookieValue, 365 * 24 * 60 * 60));
        return ResponseEntity.ok(totalLikes);
    }

    @DeleteMapping("/api/v2/likes/{boardId}")
    public ResponseEntity<Long> cancelBoardLike(
            @PathVariable("boardId") Long boardId,
            @CookieValue(name = CookieName.LIKED_BOARDS, required = false) String likedCookie,
            HttpServletResponse response) {

        if (!boardLikeHmacService.isValid(likedCookie)
                || !boardLikeHmacService.isLiked(likedCookie, boardId)) {
            throw new EntityNotFoundException(ErrorCode.BOARD_LIKE_NOT_FOUND);
        }

        Long totalLikes = boardLikeService.cancelLikes(boardId);
        String newCookieValue = boardLikeHmacService.removeLike(likedCookie, boardId);

        if (newCookieValue == null) {
            response.addCookie(CookieUtil.addCookie(CookieName.LIKED_BOARDS, "", 0));
        } else {
            response.addCookie(CookieUtil.addCookie(
                    CookieName.LIKED_BOARDS, newCookieValue, 365 * 24 * 60 * 60));
        }
        return ResponseEntity.ok(totalLikes);
    }

    @GetMapping("/api/v1/boards/{boardId}/views")
    public ResponseEntity<Long> getViews(@PathVariable("boardId") Long boardId) {
        return ResponseEntity.ok().body(boardService.getBoardDetail(boardId).getViews());
    }

    @GetMapping("/api/v1/boards/{boardId}/likes")
    public ResponseEntity<Long> getLikes(@PathVariable("boardId") Long boardId) {
        return ResponseEntity.ok().body(boardService.getBoardDetail(boardId).getLikes());
    }
}
