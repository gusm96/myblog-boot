package com.moya.myblogboot.controller;

import com.moya.myblogboot.domain.board.SearchType;
import com.moya.myblogboot.dto.board.BoardDetailResDto;
import com.moya.myblogboot.dto.board.BoardListResDto;
import com.moya.myblogboot.dto.board.BoardReqDto;
import com.moya.myblogboot.service.BoardService;
import com.moya.myblogboot.service.BoardViewCookieService;
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
public class BoardController {

    private final BoardService boardService;
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
        Long memberId = PrincipalUtil.getMemberId(principal);
        log.info("MemberId = {}", memberId);
        return ResponseEntity.ok().body(boardService.getBoardDetail(boardId));
    }

    // 게시글 작성
    @PostMapping("/api/v1/boards")
    public ResponseEntity<Long> writeBoard(@RequestBody @Valid BoardReqDto boardReqDto,
                                           Principal principal) {
        Long memberId = PrincipalUtil.getMemberId(principal);
        return ResponseEntity.ok().body(boardService.write(boardReqDto, memberId));
    }

    // 게시글 수정
    @PutMapping("/api/v1/boards/{boardId}")
    public ResponseEntity<Long> editBoard(@PathVariable("boardId") Long boardId,
                                          @RequestBody @Valid BoardReqDto boardReqDto,
                                          Principal principal) {
        Long memberId = PrincipalUtil.getMemberId(principal);
        return ResponseEntity.ok().body(boardService.edit(memberId, boardId, boardReqDto));
    }

    // 게시글 삭제
    @DeleteMapping("/api/v1/boards/{boardId}")
    public ResponseEntity<Boolean> deleteBoard(@PathVariable("boardId") Long boardId,
                                               Principal principal) {
        Long memberId = PrincipalUtil.getMemberId(principal);
        boardService.delete(boardId, memberId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    private int getPage(int page) {
        return page - 1;
    }
}