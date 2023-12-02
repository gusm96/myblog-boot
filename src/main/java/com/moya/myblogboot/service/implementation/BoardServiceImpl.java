package com.moya.myblogboot.service.implementation;

import com.moya.myblogboot.domain.board.*;
import com.moya.myblogboot.domain.category.Category;
import com.moya.myblogboot.domain.file.ImageFile;
import com.moya.myblogboot.domain.file.ImageFileDto;
import com.moya.myblogboot.domain.member.Member;
import com.moya.myblogboot.exception.custom.UnauthorizedAccessException;
import com.moya.myblogboot.repository.*;
import com.moya.myblogboot.service.BoardService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.NoResultException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BoardServiceImpl implements BoardService {
    private final BoardRepository boardRepository;
    private final ImageFileRepository imageFileRepository;
    private final BoardLikeCountRedisRepository boardLikeCountRedisRepository;
    private final MemberBoardLikeRedisRepository memberBoardLikeRedisRepository;
    private final BoardLikeRepository boardLikeRepository;

    // 페이지별 최대 게시글 수
    public static final int LIMIT = 4;

    // 모든 게시글 리스트
    @Override
    @Transactional(readOnly = true)
    public BoardListResDto retrieveBoardList(int page) {
        List<Board> boardList = boardRepository.findAll(pagination(page), LIMIT);
        Long listCount = boardRepository.findAllCount();
        try {
            List<BoardResDto> resultList = boardList.stream().map(board
                            -> BoardResDto.of(board, getBoardLikeCount(board.getId())))
                    .toList();
            return BoardListResDto.builder()
                    .list(resultList)
                    .pageCount(pageCount(listCount)).build();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("게시물 리스트를 가져오는데 실패했습니다.");
        }
    }

    // 카테고리별 게시글 리스트
    @Override
    @Transactional(readOnly = true)
    public BoardListResDto retrieveBoardListByCategory(Category category, int page){
        List<Board> boardList = boardRepository.findByCategory(category.getName(), pagination(page), LIMIT);

        // 해당하는 카테고리의 게시글 수
        Long listCount = boardRepository.findByCategoryCount(category.getName());

        List<BoardResDto> resultList = boardList.stream().map(board
                        -> BoardResDto.of(board, getBoardLikeCount(board.getId())))
                .toList();

        return BoardListResDto.builder()
                .list(resultList)
                .pageCount(pageCount(listCount))
                .build();
    }
    
    // 검색한 게시글 리스트
    @Override
    @Transactional(readOnly = true)
    public BoardListResDto retrieveBoardListBySearch (SearchType searchType, String searchContents, int page) {
        List<Board> boardList = boardRepository.findBySearch(searchType, searchContents, pagination(page), LIMIT);
        Long listCount = boardRepository.findBySearchCount(searchType, searchContents);

        List<BoardResDto> resultList = boardList.stream().map(board
                        -> BoardResDto.of(board, getBoardLikeCount(board.getId())))
                .toList();

        return BoardListResDto.builder()
                .list(resultList)
                .pageCount(pageCount(listCount))
                .build();
    }
    
    // 선택한 게시글 가져오기
    @Override
    @Transactional(readOnly = true)
    public BoardResDto retrieveBoardResponseById(Long boardId){
            return BoardResDto.builder()
                    .board(retrieveBoardById(boardId))
                    .likes(getBoardLikeCount(boardId))
                    .build();
    }
    // 게시글 수정
    @Override
    @Transactional
    public Long editBoard(Long memberId, Long boardId, String modifiedTitle, String modifiedContent, Category modifiedCategory){
        Board board = retrieveBoardById(boardId);
        if(board.getMember().getId() != memberId)
            throw new UnauthorizedAccessException("권한이 없습니다");
        // 변경 감지
        board.updateBoard(modifiedCategory, modifiedTitle, modifiedContent);
        return board.getId();
    }
    // 게시글 삭제
    @Override
    @Transactional
    public boolean deleteBoard(Long boardId, Long memberId){
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
    @Override
    @Transactional
    public Long uploadBoard(BoardReqDto boardReqDto, Member member, Category category) {
        try {
            Board newBoard = boardReqDto.toEntity(category, member);
            if (boardReqDto.getImages() != null && boardReqDto.getImages().size() > 0 ) {
                saveImageFile(boardReqDto.getImages(), newBoard);
            }
            // test
            BoardLike boardLike = BoardLike.builder().board(newBoard).build();
            newBoard.setBoardLike(boardLike);
            //
            Long boardId = boardRepository.upload(newBoard);

            // BoardLikeCount RedisHash 생성 및 저장
            if (boardId > 0) {
                createBoardLikeCount(boardId);
                return boardId;
            }
            return 0L;
        } catch (Exception e) {
            log.error("게시글 등록 중 에러 발생");
            e.printStackTrace();
            throw new RuntimeException("게시글 등록을 실패했습니다");
        }
    }

    @Override
    @Transactional
    public void saveImageFile(List<ImageFileDto> images, Board board) {
        List<ImageFile> imageFiles = images.stream()
                .map(image -> imageFileRepository.save(image.toEntity(board))).collect(Collectors.toList());
        imageFiles.forEach(board::addImageFile);
    }

    // 게시글 좋아요
    @Override
    public Long addLikeToBoard(Long memberId, Long boardId){
        if (checkBoardLikedStatus(memberId, boardId)) {
            throw new RuntimeException("이미 \"좋아요\"한 게시글 입니다.");
        }
        try {
            memberBoardLikeRedisRepository.save(memberId, boardId);
            return boardLikeCountRedisRepository.incrementBoardLikeCount(boardId);
        }catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("게시글 \"좋아요\"를 실패했습니다.");
        }
    }

    @Override
    @Transactional
    public Long addBoardLikeVersion2(Long boardId, Member member) {
        BoardLike boardLike = boardLikeRepository.findByBoardId(boardId).orElseThrow(()
                -> new NoResultException("게시글 좋아요 정보를 불러오던 중 오류가 발생했습니다."));
        if (!boardLike.getMembers().contains(member)) {
            return   boardLike.incrementLike(member);
        }else{
            throw new DuplicateKeyException("이미 \"좋아요\"한 게시글 입니다.");
        }
    }

    @Override
    public boolean checkBoardLikedStatus(Long memberId, Long boardId) {
        return memberBoardLikeRedisRepository.isMember(memberId, boardId);
    }
    @Override
    public Long deleteBoardLike(Long memberId, Long boardId) {
        if (!checkBoardLikedStatus(memberId, boardId)) {
            throw new RuntimeException("존재하지 않는다.");
        }
        try {
            memberBoardLikeRedisRepository.delete(memberId, boardId);
            return boardLikeCountRedisRepository.decrementBoardLikeCount(boardId);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("게시글 \"좋아요\"정보 삭제를 실패했습니다");
        }
    }
    @Override
    public Board retrieveBoardById(Long boardId) {
        return boardRepository.findById(boardId).orElseThrow(
                () -> new EntityNotFoundException("해당 게시글이 존재하지 않습니다.")
        );
    }
    private void createBoardLikeCount(Long boardId) {
        try {
            boardLikeCountRedisRepository.save(boardId);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("게시글 \"좋아요 수\"정보 생성 중 오류발생");
        }
    }

    private int pagination (int page){
        return page == 1 ? 0 : (page - 1) * LIMIT;
    }

    private int pageCount (Long listCount){
        return listCount > LIMIT ? (int) Math.ceil((double) listCount / LIMIT) : 1;
    }

    private Long getBoardLikeCount(Long boardId) {
        try {
            return boardLikeCountRedisRepository.findBoardLikeCount(boardId);
        } catch (NullPointerException e) {
            return 0L;
        }
    }
}
