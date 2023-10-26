package com.moya.myblogboot.service;

import com.moya.myblogboot.domain.board.*;
import com.moya.myblogboot.domain.category.Category;
import com.moya.myblogboot.domain.member.Member;
import com.moya.myblogboot.repository.BoardRepository;
import com.moya.myblogboot.repository.CategoryRepository;
import com.moya.myblogboot.repository.MemberRepository;
import jakarta.persistence.NoResultException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardService {

    private final CategoryService categoryService;
    private final BoardRepository boardRepository;
    private final MemberRepository memberRepository;
    private final CategoryRepository categoryRepository;

    // 페이지별 최대 게시글 수
    public static final int LIMIT = 5;

    // 모든 게시글 리스트
    public BoardListResDto getBoardList(int page) {
        List<Board> boardList = boardRepository.findAll(pagination(page), LIMIT);
        Long listCount = boardRepository.findAllCount();

        List<BoardResDto> resultList = boardList.stream().map(BoardResDto::of).toList();

        BoardListResDto boardListResDto = BoardListResDto.builder()
                .list(resultList)
                .pageCount(pageCount(listCount)).build();
        return boardListResDto;
    }

    // 카테고리별 게시글 리스트
    public BoardListResDto getBoardsByCategory(String category, int page){
        // 카테고리 유효성 검사
        Category findCategorty = categoryRepository.findByName(category).orElseThrow(() ->
                new NoResultException("존재하지 않는 카테고리입니다."));

        List<Board> boardList = boardRepository.findByCategory(findCategorty.getName(), pagination(page), LIMIT);

        // 해당하는 카테고리의 게시글 수
        Long listCount = boardRepository.findByCategoryCount(findCategorty.getName());

        List<BoardResDto> resultList = boardList.stream().map(BoardResDto::of).toList();
        BoardListResDto boardListResDto = BoardListResDto.builder()
                .list(resultList)
                .pageCount(pageCount(listCount))
                .build();
        return boardListResDto;
    }
    
    // 검색한 게시글 리스트
    public BoardListResDto getBoardsBySearch (SearchType searchType, String searchContents, int page) {
        List<Board> list = boardRepository.findBySearch(searchType, searchContents, pagination(page), LIMIT);
        Long listCount = boardRepository.findBySearchCount(searchType, searchContents);

        List<BoardResDto> resultList = list.stream().map(BoardResDto::of).toList();

        BoardListResDto boardListResDto = BoardListResDto.builder()
                .list(resultList)
                .pageCount(pageCount(listCount))
                .build();

        return boardListResDto;
    }
    
    // 선택한 게시글 가져오기
    public BoardResDto getBoard(Long boardId){
        try{
            Board board = findBoard(boardId);
            BoardResDto boardResDto = BoardResDto.builder().board(board).build();
            return boardResDto;
        }catch (NoResultException e){
            throw new NoSuchElementException("해당 게시글이 존재하지 않습니다.");
        }
    }
    // 게시글 수정
    @Transactional
    public Long editBoard(Long boardId, BoardReqDto boardReqDto){
        Board board = findBoard(boardId);
        Category category = categoryService.findCategory(boardReqDto.getCategory());
        // 변경 감지
        board.updateBoard(category, boardReqDto.getTitle(), boardReqDto.getContent());
        return board.getId();
    }
    // 게시글 삭제
    @Transactional
    public Boolean deleteBoard(Long boardId){
        try {
            Board board = findBoard(boardId);
            boardRepository.removeBoard(board);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    // 게시글 업로드
    @Transactional
    public long uploadBoard(BoardReqDto boardReqDto, String username) {
        Member member = memberRepository.findOne(username).orElseThrow(() ->
                new NoResultException("해당 사용자는 존재하지 않습니다."));
        Category category = categoryService.findCategory(boardReqDto.getCategory());
        Board board = boardReqDto.toEntity(category, member);
        return boardRepository.upload(board);
    }

    
    public Board findBoard(Long boardId) {
        return boardRepository.findOne(boardId).orElseThrow(
                () -> new NoResultException("해당 게시글이 존재하지 않습니다.")
        );
    }

    private int pagination (int page){
        if (page == 1) return 0;
        return (page - 1) * LIMIT;
    }

    private int pageCount (Long listCount){
        if(listCount > LIMIT){
            return (int) Math.ceil((double) listCount / LIMIT);
        }else{
            return 1;
        }
    }
}
