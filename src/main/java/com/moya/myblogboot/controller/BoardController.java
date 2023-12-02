package com.moya.myblogboot.controller;

import com.moya.myblogboot.domain.board.*;
import com.moya.myblogboot.domain.category.Category;
import com.moya.myblogboot.domain.member.Member;
import com.moya.myblogboot.domain.token.TokenInfo;
import com.moya.myblogboot.service.AuthService;
import com.moya.myblogboot.service.BoardService;
import com.moya.myblogboot.service.CategoryService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;
    private final AuthService authService;
    private final CategoryService categoryService;
    // 모든 게시글 리스트
    @GetMapping("/api/v1/boards")
    public ResponseEntity<BoardListResDto> getAllBoards(@RequestParam(name = "p", defaultValue = "1") int page) {
        return ResponseEntity.ok().body(boardService.retrieveBoardList(page));
    }

    // 카테고리별 게시글 리스트
    @GetMapping("/api/v1/boards/categories/{categoryName}")
    public ResponseEntity<BoardListResDto>   requestCategoryOfBoards(
            @PathVariable("categoryName") String categoryName,
            @RequestParam(name = "p", defaultValue = "1") int page
    ) {
        Category category = getCategoryByName(categoryName);
        return ResponseEntity.ok().body(boardService.retrieveBoardListByCategory(category, page));
    }

    // 선택한 게시글
    @GetMapping("/api/v1/boards/{boardId}")
    public ResponseEntity<BoardResDto> getBoard(@PathVariable Long boardId) {
        return ResponseEntity.ok().body(boardService.retrieveBoardResponseById(boardId));
    }

    // 게시글 작성 Post
    @PostMapping("/api/v1/management/boards")
    public ResponseEntity<Long> postBoard(HttpServletRequest request, @RequestBody @Valid BoardReqDto boardReqDto) {
        Member member = getMember(request);
        Category category = getCategory( boardReqDto.getCategory());
        return ResponseEntity.ok().body(boardService.uploadBoard(boardReqDto, member, category));
    }

    // 게시글 수정
    @PutMapping("/api/v1/management/boards/{boardId}")
    public ResponseEntity<Long> editBoard(HttpServletRequest request,
                                          @PathVariable("boardId") Long boardId,
                                          @RequestBody @Valid BoardReqDto boardReqDto) {
        Long memberId = getTokenInfo(request).getMemberPrimaryKey();
        Category category = getCategory(boardReqDto.getCategory());
        return ResponseEntity.ok().body(boardService.editBoard(memberId, boardId, boardReqDto.getTitle(), boardReqDto.getContent(), category));
    }

    // 게시글 삭제
    @DeleteMapping("/api/v1/management/boards/{boardId}")
    public ResponseEntity<Boolean> deleteBoard(HttpServletRequest request, @PathVariable("boardId") Long boardId) {
        Long memberId = getTokenInfo(request).getMemberPrimaryKey();
        return ResponseEntity.ok().body(boardService.deleteBoard(boardId, memberId));
    }

    // 게시글 검색 기능 추가
    @GetMapping("/api/v1/boards/search")
    public ResponseEntity<BoardListResDto> searchBoards(
            @RequestParam("type") SearchType searchType,
            @RequestParam("contents") String searchContents,
            @RequestParam(name = "p", defaultValue = "1") int page) {
        return ResponseEntity.ok().body(boardService.retrieveBoardListBySearch(searchType, searchContents, page));
    }

    // 게시글 좋아요 여부
    @GetMapping("/api/v1/likes/{boardId}")
    public ResponseEntity<?> requestToCheckBoardLike(HttpServletRequest request, @PathVariable("boardId") Long boardId){
        return ResponseEntity.ok().body(boardService.checkBoardLikedStatus(getTokenInfo(request).getMemberPrimaryKey(), boardId));
    }
    // 게시글 좋아요 추가 기능
    @PostMapping("/api/v1/likes/{boardId}")
    public ResponseEntity<?> requestToAddBoardLike(HttpServletRequest request, @PathVariable("boardId") Long boardId){
        return ResponseEntity.ok().body(boardService.addLikeToBoard(getTokenInfo(request).getMemberPrimaryKey(), boardId));
    }

    // 게시글 좋아요 Version2
    @PostMapping("/api/v2/likes/{boardId}")
    public ResponseEntity<Long> requestToAddBoardLikeVersion2(HttpServletRequest request, @PathVariable("boardId") Long boardId) {
        Member member = getMember(request);
        return ResponseEntity.ok().body(boardService.addBoardLikeVersion2(boardId, member));
    }

    @DeleteMapping("/api/v1/likes/{boardId}")
    public ResponseEntity<?> requestToDeleteBoardLike (HttpServletRequest request, @PathVariable("boardId") Long boardId){
        return ResponseEntity.ok().body(boardService.deleteBoardLike(getTokenInfo(request).getMemberPrimaryKey(), boardId));
    }

    private TokenInfo getTokenInfo(HttpServletRequest req){

        return authService.getTokenInfo(req.getHeader(HttpHeaders.AUTHORIZATION).split(" ")[1]);
    }

    private Member getMember(HttpServletRequest request) {
        return authService.retrieveMemberById(getTokenInfo(request).getMemberPrimaryKey());
    }
    private Category getCategory(Long categoryId) {
        return categoryService.retrieveCategoryById(categoryId);
    }
    private Category getCategoryByName (String categoryName) {
        return categoryService.retrieveCategoryByName(categoryName);
    }
}