package com.moya.myblogboot.service;

import com.moya.myblogboot.domain.board.*;
import com.moya.myblogboot.domain.category.Category;
import com.moya.myblogboot.domain.member.Member;
import com.moya.myblogboot.exception.BoardNotFoundException;
import com.moya.myblogboot.exception.DuplicateBoardLikeException;
import com.moya.myblogboot.repository.*;
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

    private final BoardRepository boardRepository;
    private final UserBoardLikeRedisRepository userBoardLikeRedisRepository;
    private final BoardLikeCountRedisRepository boardLikeCountRedisRepository;

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
    public BoardListResDto retrieveBoardListByCategory(Category category, int page){
        List<Board> boardList = boardRepository.findByCategory(category.getName(), pagination(page), LIMIT);

        // 해당하는 카테고리의 게시글 수
        Long listCount = boardRepository.findByCategoryCount(category.getName());

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
            return BoardResDto.builder()
                    .board(retrieveBoardById(boardId))
                    .likes(retrieveBoardLikeCountByBoardId(boardId).getCount())
                    .build();
    }
    // 게시글 수정
    @Transactional
    public Long editBoard(Long boardId, String modifiedTitle, String modifiedContent, Category modifiedCategory){
        Board board = retrieveBoardById(boardId);
        // 변경 감지
        board.updateBoard(modifiedCategory, modifiedTitle, modifiedContent);

        return board.getId();
    }
    // 게시글 삭제
    @Transactional
    public Boolean deleteBoard(Long boardId){
        try {
            boardRepository.removeBoard(retrieveBoardById(boardId));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    // 게시글 업로드
    @Transactional
    public Long uploadBoard(BoardReqDto boardReqDto, Member member, Category category) {
        Long boardId = boardRepository.upload(boardReqDto.toEntity(category, member));
        // BoardLikeCount 생성.
        createBoardLikeCount(boardId);
        return boardId;
    }

    private void createBoardLikeCount(Long boardId) {
        BoardLikeCount boardLikeCount = BoardLikeCount.builder()
                .id(boardId)
                .build();
        boardLikeCountRedisRepository.save(boardLikeCount);
    }

    // 게시글 좋아요
    public Long addLikeToBoard(Long memberId, Long boardId){
        if(checkBoardLikedStatus(memberId, boardId)){
            throw new DuplicateBoardLikeException("이미 \"좋아요\"한 요청 입니다.");
        }
        createUserBoardLike(memberId, boardId);
        incrementBoardLikeCount(boardId);
        return retrieveBoardLikeCountByBoardId(boardId).getCount();
    }

    private BoardLikeCount retrieveBoardLikeCountByBoardId(Long boardId) {
        return boardLikeCountRedisRepository.findById(boardId).orElseThrow(
                () -> new NoResultException("게시글 좋아요가 존재하지 않습니다."));
    }

    private void createUserBoardLike(Long memberId, Long boardId) {
        MemberBoardLike memberBoardLike = MemberBoardLike.builder()
                .id(memberId)
                .boardId(boardId)
                .build();
        userBoardLikeRedisRepository.save(memberBoardLike);
    }

    public boolean checkBoardLikedStatus(Long memberId, Long boardId) {
        try {
            retrieveMemberBoardLikeByIdAndBoardId(memberId, boardId);
            return true;
        }catch (NoSuchElementException e) {
            return false;
        }
    }

    public boolean deleteBoardLike(Long memberId, Long boardId) {
        try {
            userBoardLikeRedisRepository.delete(retrieveMemberBoardLikeByIdAndBoardId(memberId, boardId));
            decrementBoardLikeCount(boardId);
            return true;
        }catch (NoSuchElementException e){
            return false;
        }
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

    private MemberBoardLike retrieveMemberBoardLikeByIdAndBoardId (Long memberId, Long boardId){
        MemberBoardLike findMemberBoardLike = userBoardLikeRedisRepository.findById(memberId).orElseThrow(
                () -> new NoSuchElementException("게시글 좋아요 결과 없음"));
        if (findMemberBoardLike.getBoardId() == boardId) {
            return findMemberBoardLike;
        }else {
            throw new NoSuchElementException("게시글 좋아요 결과 없음");
        }
    }
    public void incrementBoardLikeCount(Long boardId) {
        BoardLikeCount boardLikeCount = retrieveBoardLikeCountByBoardId(boardId);
        boardLikeCount.increment();
        // 변경된 내용 저장
        boardLikeCountRedisRepository.save(boardLikeCount);
    }

    public void decrementBoardLikeCount(Long boardId) {
        BoardLikeCount boardLikeCount = retrieveBoardLikeCountByBoardId(boardId);
        boardLikeCount.decrement();
        // 변경된 내용 저장
        boardLikeCountRedisRepository.save(boardLikeCount);
    }
}
