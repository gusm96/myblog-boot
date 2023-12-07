package com.moya.myblogboot.service.implementation;

import com.moya.myblogboot.domain.board.*;
import com.moya.myblogboot.domain.category.Category;
import com.moya.myblogboot.domain.file.ImageFile;
import com.moya.myblogboot.domain.file.ImageFileDto;
import com.moya.myblogboot.domain.member.Member;
import com.moya.myblogboot.exception.custom.UnauthorizedAccessException;
import com.moya.myblogboot.repository.*;
import com.moya.myblogboot.service.AuthService;
import com.moya.myblogboot.service.BoardService;
import com.moya.myblogboot.service.CategoryService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Slf4j
@Transactional(readOnly = true)
@Service
@RequiredArgsConstructor
public class BoardServiceImpl implements BoardService {
    private final CategoryService categoryService;
    private final AuthService authService;
    private final BoardRepository boardRepository;
    private final ImageFileRepository imageFileRepository;
    private final BoardLikeRedisRepository boardLikeRedisRepository;
    // 페이지별 최대 게시글 수
    private static final int LIMIT = 4;

    // 모든 게시글 리스트
    @Override
    public BoardListResDto retrieveBoardList(int page) {
        PageRequest pageRequest = PageRequest.of(page, LIMIT,Sort.by(Sort.Direction.DESC, "uploadDate"));
        // 페이지 별 게시글 조회
        Page<Board> boards = boardRepository.findAll(pageRequest);
        // 조회한 Board Entity List를 DTO 객체로 변환.
        List<BoardResDto> resultList = boards.stream().map(board
                        -> BoardResDto.of(board, getBoardLikeCount(board.getId())))
                .toList();
        // 화면에 보여질 List와 개시글 총 개수 반환.
        return BoardListResDto.builder()
                .list(resultList)
                .totalPage(boards.getTotalPages())
                .build();
    }

    // 카테고리별 게시글 리스트
    @Override
    public BoardListResDto retrieveBoardListByCategory(String categoryName, int page){
        // Category 조회
        Category category = categoryService.retrieveCategoryByName(categoryName);

        PageRequest pageRequest = PageRequest.of(page, LIMIT, Sort.by(Sort.Direction.DESC, "uploadDate"));
        Page<Board> boards = boardRepository.findAllByCategory(category, pageRequest);

        // DTO 객체로 변환
        List<BoardResDto> resultList = boards.stream().map(board
                        -> BoardResDto.of(board, getBoardLikeCount(board.getId())))
                .toList();

        return BoardListResDto.builder()
                .list(resultList)
                .totalPage(boards.getTotalPages())
                .build();
    }

    // 검색한 게시글 리스트
    /*@Override
    public BoardListResDto retrieveBoardListBySearch (SearchType searchType, String searchContents, int page) {
        // 검색어 + 페이지 게시글 조회
        List<Board> boardList = boardRepository.findBySearch(searchType, searchContents, page, LIMIT);
        // 검색된 게시글의 수
        Long listCount = boardRepository.findBySearchCount(searchType, searchContents);
        // DTO 객체로 변환
        List<BoardResDto> resultList = boardList.stream().map(board
                        -> BoardResDto.of(board, getBoardLikeCount(board.getId())))
                .toList();
        return BoardListResDto.builder()
                .list(resultList)
                .totalPage(pageCount(listCount))
                .build();
    }*/

    // 게시글 업로드
    @Override
    @Transactional
    public Long uploadBoard(BoardReqDto boardReqDto, Long memberId) {
        Member member = authService.retrieveMemberById(memberId);
        Category category = categoryService.retrieveCategoryById(boardReqDto.getCategory());
        Board newBoard = boardReqDto.toEntity(category, member);
        try {
            if (boardReqDto.getImages() != null && boardReqDto.getImages().size() > 0 ) {
                saveImageFile(boardReqDto.getImages(), newBoard);
            }
            Board result = boardRepository.save(newBoard);
            category.addBoard(result);
            return result.getId();
        } catch (Exception e) {
            log.error("게시글 등록 중 에러 발생");
            e.printStackTrace();
            throw new RuntimeException("게시글 등록을 실패했습니다");
        }
    }

    // 게시글 상세
    @Override
    public BoardDetailResDto boardToResponseDto(Long boardId) {
        Board findBoard = retrieveBoardById(boardId);
        return BoardDetailResDto.builder()
                .board(findBoard)
                .likes(getBoardLikeCount(boardId))
                .build();
    }

    // 게시글 수정
    @Override
    @Transactional
    public Long editBoard(Long memberId, Long boardId, BoardReqDto modifiedDto){
        // Entity 조회
        Board board = retrieveBoardById(boardId);
        if(board.getMember().getId() != memberId)
            throw new UnauthorizedAccessException("권한이 없습니다");
        Category modifiedCategory = categoryService.retrieveCategoryById(modifiedDto.getCategory());
        board.updateBoard(modifiedCategory, modifiedDto.getTitle(), modifiedDto.getContent()); // 변경감지
        return board.getId();
    }

    // 게시글 삭제
    @Override
    @Transactional
    public boolean deleteBoard(Long boardId, Long memberId){
        // Entity 조회
        Board board = retrieveBoardById(boardId);
        if (board.getMember().getId() == memberId) {
            board.setDeleteDate();
            return true;
        }else {
            throw new UnauthorizedAccessException("권한이 없습니다.");
        }
    }

    // 게시글 좋아요 Redis
    @Override
    public Long addLikeToBoard(Long memberId, Long boardId){
        if (checkBoardLikedStatus(memberId, boardId)) {
            throw new RuntimeException("이미 \"좋아요\"한 게시글 입니다.");
        }
        try {
            boardLikeRedisRepository.add(boardId, memberId);
            return getBoardLikeCount(boardId);
        }catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("게시글 \"좋아요\"를 실패했습니다.");
        }
    }

    // 게시글 좋아요 여부 체크
    @Override
    public boolean checkBoardLikedStatus(Long memberId, Long boardId) {
        return boardLikeRedisRepository.isMember(boardId, memberId);
    }

    // 게시글 좋아요 취소 - Redis
    @Override
    public Long deleteBoardLike(Long memberId, Long boardId) {
        if (!checkBoardLikedStatus(memberId, boardId)) {
            throw new NoSuchElementException("잘못된 요청입니다.");
        }
        try {
            boardLikeRedisRepository.cancel(boardId, memberId);
            return getBoardLikeCount(boardId);
        } catch (Exception e) {
            log.error("게시글 \"좋아요\" 정보 삭제 실패");
            throw new RuntimeException("게시글 \"좋아요\"취소를 실패했습니다");
        }
    }

    // 게시글 Entity 조회
    @Override
    public Board retrieveBoardById(Long boardId) {
        try {
            return boardRepository.findById(boardId).orElseThrow(
                    () -> new EntityNotFoundException("해당 게시글이 존재하지 않습니다.")
            );
        } catch (InvalidDataAccessApiUsageException e) {
            e.printStackTrace();
            throw new RuntimeException("게시글 조회중 오류 발생.");
        }
    }


    // 게시글 좋아요 수 조회 - Redis
    private Long getBoardLikeCount(Long boardId) {
        return boardLikeRedisRepository.getCount(boardId);
    }
    // 이미지 파일 저장
    private void saveImageFile(List<ImageFileDto> images, Board board) {
        List<ImageFile> imageFiles = images.stream()
                .map(image -> imageFileRepository.save(image.toEntity(board))).collect(Collectors.toList());
        imageFiles.forEach(board::addImageFile);
    }
}
