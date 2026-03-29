package com.moya.myblogboot.controller;

import com.moya.myblogboot.domain.board.*;
import com.moya.myblogboot.dto.board.BoardDetailResDto;
import com.moya.myblogboot.dto.board.BoardListResDto;
import com.moya.myblogboot.dto.board.BoardReqDto;
import com.moya.myblogboot.service.BoardLikeService;
import com.moya.myblogboot.service.BoardService;
import com.moya.myblogboot.service.BoardViewCookieService;
import com.moya.myblogboot.utils.CookieUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

import static com.moya.myblogboot.constants.CookieName.*;

@Slf4j
@RestController
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;
    private final BoardLikeService boardLikeService;
    private final BoardViewCookieService boardViewCookieService;

    // 모든 게시글 리스트
    @GetMapping("/api/v1/boards")
    public ResponseEntity<BoardListResDto> getAllBoards(@RequestParam(name = "p", defaultValue = "1") int page) {
        return ResponseEntity.ok().body(boardService.retrieveAll(getPage(page)));
    }

    // 카테고리별 게시글 리스트
    @GetMapping("/api/v1/boards/category")
    public ResponseEntity<BoardListResDto> getCategoryBoards(
            @RequestParam("c") String category,
            @RequestParam(name = "p", defaultValue = "1") int page
    ) {
        return ResponseEntity.ok().body(boardService.retrieveAllByCategory(category, getPage(page)));
    }

    // 검색 결과 게시글 리스트
    @GetMapping("/api/v1/boards/search")
    public ResponseEntity<BoardListResDto> getSearchedBoards(
            @RequestParam("type") SearchType searchType,
            @RequestParam("contents") String searchContents,
            @RequestParam(name = "p", defaultValue = "1") int page) {
        return ResponseEntity.ok().body(boardService.retrieveAllBySearched(searchType, searchContents, getPage(page)));
    }

    // 게시글 상세 조회 V8 — Cookie + HMAC 기반 Stateless 중복 방지
    @GetMapping("/api/v8/boards/{boardId}")
    public ResponseEntity<BoardDetailResDto> getBoardDetailV8(@PathVariable("boardId") Long boardId,
                                                              HttpServletRequest request,
                                                              HttpServletResponse response) {
        Cookie cookie = CookieUtil.findCookie(request, VIEWED_BOARDS);
        String cookieValue = (cookie != null) ? cookie.getValue() : null;

        boolean valid = boardViewCookieService.isValid(cookieValue);

        if (!valid || !boardViewCookieService.isViewed(cookieValue, boardId)) {
            BoardDetailResDto dto = boardService.getBoardDetailAndIncrementViews(boardId);
            String newValue = boardViewCookieService.addViewed(valid ? cookieValue : null, boardId);
            response.addCookie(CookieUtil.addCookie(VIEWED_BOARDS, newValue, boardViewCookieService.secondsUntilMidnight()));
            return ResponseEntity.ok(dto);
        }

        return ResponseEntity.ok(boardService.getBoardDetail(boardId));
    }

    // 게시글 상세 관리자용
    @GetMapping("/api/v1/management/boards/{boardId}")
    public ResponseEntity<BoardDetailResDto> getBoardDetailForAdmin(@PathVariable("boardId") Long boardId,
                                                                    Principal principal) {
        Long memberId = getMemberId(principal);
        log.info("MemberId = {}", memberId);
        return ResponseEntity.ok().body(boardService.getBoardDetail(boardId));
    }

    // 게시글 작성 Post
    @PostMapping("/api/v1/boards")
    public ResponseEntity<Long> writeBoard(@RequestBody @Valid BoardReqDto boardReqDto,
                                           Principal principal) {
        Long memberId = getMemberId(principal);
        return ResponseEntity.ok().body(boardService.write(boardReqDto, memberId));

    }

    // 게시글 수정 PUT
    @PutMapping("/api/v1/boards/{boardId}")
    public ResponseEntity<Long> editBoard(@PathVariable("boardId") Long boardId,
                                          @RequestBody @Valid BoardReqDto boardReqDto,
                                          Principal principal) {
        Long memberId = getMemberId(principal);
        return ResponseEntity.ok().body(boardService.edit(memberId, boardId, boardReqDto));
    }

    // 게시글 삭제 DELETE
    @DeleteMapping("/api/v1/boards/{boardId}")
    public ResponseEntity<Boolean> deleteBoard(@PathVariable("boardId") Long boardId,
                                               Principal principal) {
        Long memberId = getMemberId(principal);
        boardService.delete(boardId, memberId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    // 삭제 예정 게시글 리스트 GET
    @GetMapping("/api/v1/deleted-boards")
    public ResponseEntity<?> getDeletedBoards(@RequestParam(name = "p", defaultValue = "1") int page) {
        return ResponseEntity.ok().body(boardService.retrieveAllDeleted(getPage(page)));
    }

    // 게시글 삭제 취소 PUT
    @PutMapping("/api/v1/deleted-boards/{boardId}")
    public ResponseEntity<?> cancelDeletedBoard(@PathVariable("boardId") Long boardId,
                                                Principal principal) {
        Long memberId = getMemberId(principal);
        boardService.undelete(boardId, memberId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    // 게시글 영구 삭제 DELETE
    @DeleteMapping("/api/v1/deleted-boards/{boardId}")
    public ResponseEntity<?> deleteBoardPermanently(@PathVariable("boardId") Long boardId) {
        boardService.deletePermanently(boardId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    // 게시글 좋아요 여부 확인 GET
    @GetMapping("/api/v2/likes/{boardId}")
    public ResponseEntity<?> checkBoardLike(@PathVariable("boardId") Long boardId,
                                            Principal principal) {
        Long memberId = getMemberId(principal);
        return ResponseEntity.ok().body(boardLikeService.isLiked(boardId, memberId));
    }

    // 게시글 좋아요 POST
    @PostMapping("/api/v2/likes/{boardId}")
    public ResponseEntity<Long> addBoardLike(@PathVariable("boardId") Long boardId,
                                             Principal principal) {
        Long memberId = getMemberId(principal);
        return ResponseEntity.ok().body(boardLikeService.addLikes(boardId, memberId));
    }

    // 게시글 좋아요 취소 DELETE
    @DeleteMapping("/api/v2/likes/{boardId}")
    public ResponseEntity<?> cancelBoardLike(@PathVariable("boardId") Long boardId,
                                             Principal principal) {
        Long memberId = getMemberId(principal);
        return ResponseEntity.ok().body(boardLikeService.cancelLikes(boardId, memberId));
    }

    // 조회수 갱신용
    @GetMapping("/api/v1/boards/{boardId}/views")
    public ResponseEntity<Long> getViews(@PathVariable("boardId") Long boardId) {
        return ResponseEntity.ok().body(boardService.getBoardDetail(boardId).getViews());
    }

    // 좋아요수 갱신용
    @GetMapping("/api/v1/boards/{boardId}/likes")
    public ResponseEntity<Long> getLikes(@PathVariable("boardId") Long boardId) {
        return ResponseEntity.ok().body(boardService.getBoardDetail(boardId).getLikes());
    }

    private int getPage(int page) {
        return page - 1;
    }

    private Long getMemberId(Principal principal) {
        Long memberId = -1L;
        if (principal instanceof UsernamePasswordAuthenticationToken) {
            memberId = (Long) ((UsernamePasswordAuthenticationToken) principal).getPrincipal();
        }
        return memberId;
    }

}