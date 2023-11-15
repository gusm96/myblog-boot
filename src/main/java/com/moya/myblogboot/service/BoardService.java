package com.moya.myblogboot.service;

import com.moya.myblogboot.domain.board.*;
import com.moya.myblogboot.domain.category.Category;
import com.moya.myblogboot.domain.member.Member;
import com.moya.myblogboot.exception.UnauthorizedAccessException;
import com.moya.myblogboot.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardService {

    private final BoardRepository boardRepository;
    private final RedisTemplate<String,Object> redisTemplate;
    private static final String MEMBER_BOARD_LIKE_KEY = "memberBoardLike:";
    private static final String BOARD_LIKE_COUNT_KEY = "boardLikeCount:";

    // 페이지별 최대 게시글 수
    public static final int LIMIT = 10;

    // 모든 게시글 리스트
    public BoardListResDto retrieveBoardList(int page) {
        List<Board> boardList = boardRepository.findAll(pagination(page), LIMIT);
        Long listCount = boardRepository.findAllCount();
        List<BoardResDto> resultList = boardList.stream().map(board
                -> BoardResDto.of(board, getBoardLikeCount(board.getId())))
                .toList();

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

        List<BoardResDto> resultList = boardList.stream().map(board
                        -> BoardResDto.of(board, getBoardLikeCount(board.getId())))
                .toList();

        BoardListResDto boardListResDto = BoardListResDto.builder()
                .list(resultList)
                .pageCount(pageCount(listCount))
                .build();
        return boardListResDto;
    }
    
    // 검색한 게시글 리스트
    public BoardListResDto retrieveBoardListBySearch (SearchType searchType, String searchContents, int page) {
        List<Board> boardList = boardRepository.findBySearch(searchType, searchContents, pagination(page), LIMIT);
        Long listCount = boardRepository.findBySearchCount(searchType, searchContents);

        List<BoardResDto> resultList = boardList.stream().map(board
                        -> BoardResDto.of(board, getBoardLikeCount(board.getId())))
                .toList();

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
                    .likes(getBoardLikeCount(boardId))
                    .build();
    }
    // 게시글 수정
    @Transactional
    public Long editBoard(Long boardId, String modifiedTitle, String modifiedContent,Long memberId, Category modifiedCategory){
        Board board = retrieveBoardById(boardId);
        if(board.getMember().getId() != memberId)
            throw new UnauthorizedAccessException("권한이 없습니다");
        // 변경 감지
        board.updateBoard(modifiedCategory, modifiedTitle, modifiedContent);
        return board.getId();
    }
    // 게시글 삭제
    @Transactional
    public Boolean deleteBoard(Long boardId, Long memberId){
        Board board = retrieveBoardById(boardId);
        if (board.getMember().getId() == memberId) {
            try {
                boardRepository.removeBoard(board);
                return true;
            } catch (Exception e) {
                log.error("게시글 삭제 중 에러 발생");
                throw new RuntimeException("게시글 삭제를 실패했습니다.");
            }
        }else {
            throw new UnauthorizedAccessException("권한이 없습니다.");
        }
    }
    // 게시글 업로드
    @Transactional
    public String uploadBoard(BoardReqDto boardReqDto, Member member, Category category) {
        try {
            Long boardId = boardRepository.upload(boardReqDto.toEntity(category, member));
            // BoardLikeCount RedisHash 생성 및 저장
            if (boardId > 0) {
                createBoardLikeCount(boardId);
                return "게시글 등록 성공";
            }
            return "게시글 등록 실패";
        } catch (Exception e) {
            log.error("게시글 등록 중 에러 발생");
            throw new RuntimeException("게시글 등록을 실패했습니다");
        }
    }


    private void createBoardLikeCount(Long boardId) {
        HashOperations<String, String, Object> hashOperations = redisTemplate.opsForHash();
        String key = BOARD_LIKE_COUNT_KEY + boardId;
        String hashKey = "count";
        if(hashOperations.get(key,hashKey) == null){
            hashOperations.put(key, hashKey, 0L);
        }
    }

    // 게시글 좋아요
    @Transactional
    public Long addLikeToBoard(Long memberId, Long boardId){
        if(checkBoardLikedStatus(memberId, boardId))
            throw new DuplicateKeyException("이미 \"좋아요\"했습니다.");
        createMemberBoardLike(memberId, boardId);
        incrementBoardLikeCount(boardId);
        return getBoardLikeCount(boardId);
    }

    private Long getBoardLikeCount(Long boardId) {
        HashOperations<String, String, Object> hashOperations = redisTemplate.opsForHash();
        String key = BOARD_LIKE_COUNT_KEY + boardId;
        String hashKey = "count";
        return (Long) hashOperations.get(key, hashKey);
    }
    private void createMemberBoardLike(Long memberId, Long boardId) {
        HashOperations<String, String, Object> hashOperations = redisTemplate.opsForHash();
        String key = MEMBER_BOARD_LIKE_KEY + memberId;
        String hashKey = "boardId";
        hashOperations.put(key, hashKey, boardId);
    }

    public boolean checkBoardLikedStatus(Long memberId, Long boardId) {
        HashOperations<String, String, Object> hashOperations = redisTemplate.opsForHash();
        String key = MEMBER_BOARD_LIKE_KEY + memberId;
        String hashKey = "boardId";
        if(hashOperations.get(key,hashKey) == boardId){
            return true;
        }
        return false;
    }

    public boolean deleteBoardLike(Long memberId, Long boardId) {
        HashOperations<String, String, Object> hashOperations = redisTemplate.opsForHash();
        String key = MEMBER_BOARD_LIKE_KEY + memberId;
        String hashKey = "boardId";

        try {
            decrementBoardLikeCount(boardId);
            hashOperations.delete(key, hashKey, boardId);
            return true;
        }catch (Exception e){
            log.error("게시글 좋아요 삭제 중 에러 발생");
            throw new RuntimeException("게시글 \"좋아요\" 정보를 조회 및 삭제할 수 없습니다.");
        }
    }


    public Board retrieveBoardById(Long boardId) {
        return boardRepository.findById(boardId).orElseThrow(
                () -> new EntityNotFoundException("해당 게시글이 존재하지 않습니다.")
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

    private void updateBoardLikeCount(Long boardId, Long delta) {
        HashOperations<String, String, Object> hashOperations = redisTemplate.opsForHash();
        String key = BOARD_LIKE_COUNT_KEY + boardId;
        String hashKey = "count";

        Object countObject = hashOperations.get(key, hashKey);
        if (countObject != null) {
            Long currentCount = (Long) countObject;
            Long newCount = currentCount + delta;
            hashOperations.put(key, hashKey, newCount);
        }
    }

    private void incrementBoardLikeCount(Long boardId) {
        updateBoardLikeCount(boardId, 1L);
    }

    private void decrementBoardLikeCount(Long boardId) {
        updateBoardLikeCount(boardId, -1L);
    }
}
