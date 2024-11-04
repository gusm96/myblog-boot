package com.moya.myblogboot.controller;

import com.moya.myblogboot.domain.board.*;
import com.moya.myblogboot.service.BoardLikeService;
import com.moya.myblogboot.service.BoardService;
import com.moya.myblogboot.service.UserViewedBoardService;
import com.moya.myblogboot.utils.CookieUtil;
import jakarta.persistence.EntityNotFoundException;
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

@Slf4j
@RestController
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;
    private final BoardLikeService boardLikeService;
    private final UserViewedBoardService userViewedBoardService;

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

    // 게시글 상세 V4
    @GetMapping("/api/v4/boards/{boardId}")
    public ResponseEntity<BoardDetailResDto> getBoardDetail(@PathVariable("boardId") Long boardId) {
        return ResponseEntity.ok().body(boardService.retrieveAndIncrementViewsDto(boardId));
    }

    // 게시글 상세 조회 V5
    @GetMapping("/api/v5/boards/{boardId}")
    public ResponseEntity<BoardDetailResDto> getBoardDetailV5(@PathVariable("boardId") Long boardId, HttpServletRequest req) {
        // Client IP를 가져온다.
        String clientIp = req.getRemoteAddr();
        String key = "boardId:" + boardId + "clientIp:" + clientIp;
        BoardDetailResDto boardDetailResDto;
        // Redis에서 조회
        if (boardService.isDuplicateBoardViewCount(key)) {
            boardDetailResDto = boardService.retrieveDto(boardId);
        } else {
            boardDetailResDto = boardService.retrieveAndIncrementViewsDto(boardId);
        }
        return ResponseEntity.ok().body(boardDetailResDto);
    }

    // 게시글 상세 조회 V6
    @GetMapping("/api/v6/boards/{boardId}")
    public ResponseEntity<BoardDetailResDto> getBoardDetailV6(
            @PathVariable("boardId") Long boardId, HttpServletRequest req, HttpServletResponse res) {
        Cookie oldCookie = CookieUtil.findCookie(req, "view_count");
        BoardDetailResDto result;

        if (oldCookie != null) {
            log.info("쿠키가 존재함");

            // 쿠키가 존재하지만 해당 게시글 ID가 포함되지 않은 경우
            if (!oldCookie.getValue().contains("[" + boardId + "]")) {
                log.info("현재 쿠키 값: {}", oldCookie.getValue());

                // 게시글 ID를 쿠키 값에 추가
                oldCookie.setValue(oldCookie.getValue() + "[" + boardId + "]");
                log.info("업데이트된 쿠키 값: {}", oldCookie.getValue());

                oldCookie.setPath("/");
                if (oldCookie.getMaxAge() <= 0) {
                    oldCookie.setMaxAge(60 * 60 * 24); // 24시간으로 설정
                }

                // 조회수 증가 및 데이터 조회
                result = boardService.retrieveAndIncrementViewsDto(boardId);
            } else {
                log.info("쿠키가 존재하고 해당 게시글도 존재함");
                // 조회수 증가 없이 데이터만 조회
                result = boardService.retrieveDto(boardId);
            }

            // 변경된 쿠키를 응답에 추가
            res.addCookie(oldCookie);
        } else {
            log.info("쿠키가 존재하지 않음");

            // 새로운 쿠키 생성
            Cookie newCookie = new Cookie("view_count", "[" + boardId + "]");
            newCookie.setPath("/");
            newCookie.setMaxAge(60 * 60 * 24); // 24시간으로 설정
            res.addCookie(newCookie);

            // 조회수 증가 및 데이터 조회
            result = boardService.retrieveAndIncrementViewsDto(boardId);
        }

        return ResponseEntity.ok().body(result);
    }

    @GetMapping("/api/v7/boards/{boardId}")
    public ResponseEntity<BoardDetailResDto> getBoardDetailV7(@PathVariable("boardId") Long boardId, HttpServletRequest req) {
        Cookie userNumCookie = CookieUtil.findCookie(req, "user_n");
        Long userNum = Long.parseLong(userNumCookie.getValue());
        BoardDetailResDto boardDetailResDto;
        try {
            if (!userViewedBoardService.isViewedBoard(userNum, boardId)) {
                boardDetailResDto = boardService.retrieveAndIncrementViewsDto(boardId);
                userViewedBoardService.addViewedBoard(userNum, boardId);
            } else {
                boardDetailResDto = boardService.retrieveDto(boardId);
            }
        } catch (Exception e) {
            throw new EntityNotFoundException(e.getMessage());
        }
        return ResponseEntity.ok().body(boardDetailResDto);
    }

    // 게시글 상세 관리자용
    @GetMapping("/api/v1/management/boards/{boardId}")
    public ResponseEntity<BoardDetailResDto> getBoardDetailForAdmin(@PathVariable("boardId") Long boardId, Principal principal) {
        Long memberId = getMemberId(principal);
        log.info("MemberId = {}", memberId);
        return ResponseEntity.ok().body(boardService.retrieveDto(boardId));
    }

    // 게시글 작성 Post
    @PostMapping("/api/v1/boards")
    public ResponseEntity<Long> writeBoard(@RequestBody @Valid BoardReqDto boardReqDto, Principal principal) {
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
    public ResponseEntity<Boolean> deleteBoard(@PathVariable("boardId") Long boardId, Principal principal) {
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
    public ResponseEntity<?> cancelDeletedBoard(@PathVariable("boardId") Long boardId, Principal principal) {
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
    public ResponseEntity<?> checkBoardLike(@PathVariable("boardId") Long boardId, Principal principal) {
        Long memberId = getMemberId(principal);
        return ResponseEntity.ok().body(boardLikeService.isLiked(boardId, memberId));
    }

    // 게시글 좋아요 POST
    @PostMapping("/api/v2/likes/{boardId}")
    public ResponseEntity<Long> addBoardLike(@PathVariable("boardId") Long boardId, Principal principal) {
        Long memberId = getMemberId(principal);
        return ResponseEntity.ok().body(boardLikeService.addLikes(boardId, memberId));
    }

    // 게시글 좋아요 취소 DELETE
    @DeleteMapping("/api/v2/likes/{boardId}")
    public ResponseEntity<?> cancelBoardLike(@PathVariable("boardId") Long boardId, Principal principal) {
        Long memberId = getMemberId(principal);
        return ResponseEntity.ok().body(boardLikeService.cancelLikes(boardId, memberId));
    }

    // 조회수 갱신용
    @GetMapping("/api/v1/boards/{boardId}/views")
    public ResponseEntity<Long> getViews(@PathVariable("boardId") Long boardId) {
        return ResponseEntity.ok().body(boardService.retrieveDto(boardId).getViews());
    }

    // 좋아요수 갱신용
    @GetMapping("/api/v1/boards/{boardId}/likes")
    public ResponseEntity<Long> getLikes(@PathVariable("boardId") Long boardId) {
        return ResponseEntity.ok().body(boardService.retrieveDto(boardId).getLikes());
    }


    private Long getMemberId(Principal principal) {
        Long memberId = -1L;
        if (principal instanceof UsernamePasswordAuthenticationToken) {
            memberId = (Long) ((UsernamePasswordAuthenticationToken) principal).getPrincipal();
        }
        return memberId;
    }

    private int getPage(int page) {
        return page - 1;
    }


}