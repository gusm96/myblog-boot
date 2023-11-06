package com.moya.myblogboot.service;

import com.moya.myblogboot.domain.board.*;
import com.moya.myblogboot.domain.category.Category;
import com.moya.myblogboot.domain.member.Member;
import com.moya.myblogboot.exception.BoardNotFoundException;
import com.moya.myblogboot.repository.BoardRepository;
import com.moya.myblogboot.repository.CategoryRepository;
import com.moya.myblogboot.repository.MemberRepository;
import jakarta.persistence.NoResultException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardService {

    private final CategoryService categoryService;
    private final BoardRepository boardRepository;
    private final MemberRepository memberRepository;
    private final CategoryRepository categoryRepository;
    private final RedisTemplate<String, Object> template;


    // 페이지별 최대 게시글 수
    public static final int LIMIT = 10;

    // 모든 게시글 리스트
    public BoardListResDto retrieveBoardList(int page) {
        List<Board> boardList = boardRepository.findAll(pagination(page), LIMIT);
        Long listCount = boardRepository.findAllCount();

        List<BoardResDto> resultList = boardList.stream().map(BoardResDto::of).toList();

        BoardListResDto boardListResDto = BoardListResDto.builder()
                .list(resultList)
                .pageCount(pageCount(listCount)).build();
        return boardListResDto;
    }

    // 카테고리별 게시글 리스트
    public BoardListResDto retrieveBoardListByCategory(String category, int page){
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
    public BoardListResDto retrieveBoardListBySearch (SearchType searchType, String searchContents, int page) {
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
    public BoardResDto retrieveBoardResponseById(Long boardId){
            Board board = retrieveBoardById(boardId);
            return BoardResDto.builder().board(board).build();
    }
    // 게시글 수정
    @Transactional
    public Long editBoard(Long boardId, BoardReqDto boardReqDto){
        Board board = retrieveBoardById(boardId);
        Category category = categoryService.findCategory(boardReqDto.getCategory());
        // 변경 감지
        board.updateBoard(category, boardReqDto.getTitle(), boardReqDto.getContent());
        return board.getId();
    }
    // 게시글 삭제
    @Transactional
    public Boolean deleteBoard(Long boardId){
        try {
            Board board = retrieveBoardById(boardId);
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
        Member member = retrieveMemberByUsername(username);
        Category category = categoryService.findCategory(boardReqDto.getCategory());
        Board board = boardReqDto.toEntity(category, member);
        return boardRepository.upload(board);
    }
    // 게시글 좋아요
    public boolean addLikeToBoard(String username, Long boardIdx){
        boolean result = false;
        Member findMember = retrieveMemberByUsername(username);
        Board findBoard = retrieveBoardById(boardIdx);
        String key = "board_id: " + findBoard.getId();
        BoardLike boardLike ;
        return result;
    }

    private Member retrieveMemberByUsername(String username) {
        return memberRepository.findOne(username).orElseThrow(() -> new UsernameNotFoundException("존재하지 않는 회원입니다."));
    }
    public Board retrieveBoardById(Long boardId) {
        return boardRepository.findOne(boardId).orElseThrow(
                () -> new BoardNotFoundException("해당 게시글이 존재하지 않습니다.")
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
