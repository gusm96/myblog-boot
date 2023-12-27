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
import com.moya.myblogboot.service.FileUploadService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
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
    private final BoardRedisRepository boardRedisRepository;
    private final FileUploadService fileUploadService;
    // 페이지별 최대 게시글 수
    private static final int LIMIT = 4;

    // 모든 게시글 리스트
    @Override
    public BoardListResDto retrieveBoardList(int page) {
        PageRequest pageRequest = PageRequest.of(page, LIMIT,Sort.by(Sort.Direction.DESC, "createDate"));
        Page<Board> boards = boardRepository.findAll(BoardStatus.VIEW, pageRequest);
        return convertToBoardListResDto(boards);
    }

    // 카테고리별 게시글 리스트
    @Override
    public BoardListResDto retrieveBoardListByCategory(String categoryName, int page){
        PageRequest pageRequest = PageRequest.of(page, LIMIT, Sort.by(Sort.Direction.DESC, "createDate"));
        Page<Board> boards = boardRepository.findAllByCategory(categoryName, pageRequest);
        return convertToBoardListResDto(boards);
    }

    // 검색한 게시글 리스트
    @Override
    public BoardListResDto retrieveBoardListBySearch (SearchType searchType, String searchContents, int page) {
        PageRequest pageRequest = PageRequest.of(page, LIMIT);
        // 검색어 + 페이지 게시글 조회
        Page<Board> boards = boardRepository.findBySearchType(pageRequest, searchType, searchContents);
        return convertToBoardListResDto(boards);
    }
    // 삭제 예정 게시글 리스트
    @Override
    public BoardListResDto retrieveDeletedBoards(int page) {
        PageRequest pageRequest = PageRequest.of(page, LIMIT, Sort.by(Sort.Direction.DESC, "deleteDate"));
        Page<Board> boards = boardRepository.findByDeletionStatus(pageRequest);
        return convertToBoardListResDto(boards);
    }

    private BoardListResDto convertToBoardListResDto(Page<Board> boards){
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

    // 게시글 상세 조회 V2
    @Override
    @Transactional
    public BoardDetailResDto boardToResponseDto(Long boardId) {
        // Board Entity 조회
        Board findBoard = retrieveBoardById(boardId);
        // Redis에 저장된 좋아요 수
        Long likes = getBoardLikeCount(boardId);
        // Redis에서 조회수 가져오기
        Long views = getViews(findBoard);

        // 응답용 DTO 객체로 변환
        return BoardDetailResDto.builder()
                .board(findBoard)
                .likes(likes)
                .views(views)
                .build();
    }
    // Redis에서 조회수 가져오기
    private Long getViews(Board board) {
        Long views = boardRedisRepository.getViews(board.getId());
        // 조회수 갱신 로직
        if (views == null || views < board.getViews()) {
            // 조회수가 null 이거나 DB에 저장된 값보다 작은 경우 캐시에 DB 데이터 저장
            views = boardRedisRepository.setViews(board.getId(), board.getViews() + 1L);
        } else {
            // 조회수 증가 후 결과 값 반환
            views = boardRedisRepository.viewsIncrement(board.getId());
        }
        return views;
    }

    // 게시글 상세 조회 V3
    @Override
    public BoardResDtoV2 retrieveBoardDetail(Long boardId) {
        Optional<BoardForRedis> boardForRedis = boardRedisRepository.findById(boardId);
        // Memory에 데이터 없으면 DB에서 조회
        if(boardForRedis.isEmpty()){
            Board board = retrieveBoardById(boardId);
            BoardForRedis saveBoard = boardRedisRepository.save(board);
            return BoardResDtoV2.builder().boardForRedis(saveBoard).build();
        }
        return BoardResDtoV2.builder().boardForRedis(boardForRedis.get()).build();
    }

    // 게시글 수정
    @Override
    @Transactional
    public Long editBoard(Long memberId, Long boardId, BoardReqDto modifiedDto){
        // Entity 조회
        Board board = retrieveBoardById(boardId);
        if(!board.getMember().getId().equals(memberId))
            throw new UnauthorizedAccessException("권한이 없습니다");
        Category modifiedCategory = categoryService.retrieveCategoryById(modifiedDto.getCategory());
        board.updateBoard(modifiedCategory, modifiedDto.getTitle(), modifiedDto.getContent()); // 변경감지
        updateBoardForRedis(board);
        return board.getId();
    }

    // 게시글 삭제
    @Override
    @Transactional
    public boolean deleteBoard(Long boardId, Long memberId){
        // Entity 조회
        Board board = retrieveBoardById(boardId);
        if (board.getMember().getId().equals(memberId)) {
            board.deleteBoard(); // 15일 이후 자동 삭제
            updateBoardForRedis(board);
            return true;
        }else {
            throw new UnauthorizedAccessException("권한이 없습니다.");
        }
    }

    @Override
    @Transactional
    public void deletePermanently(LocalDateTime thresholdDate) {
        List<Board> boards = boardRepository.findByDeleteDate(thresholdDate);
        for (Board b : boards) {
            // 이미지 파일 찾아서 S3에서 삭제
            b.getImageFiles().stream().map(imageFile ->
                    fileUploadService.deleteImageFile(imageFile.getFileName())
            );
            try {
                boardRepository.delete(b);
            } catch (Exception e) {
                throw new RuntimeException("게시글 삭제를 실패했습니다.");
            }
        }
    }

    @Override
    @Transactional
    public void undeleteBoard(Long boardId) {
        Board board = retrieveBoardById(boardId);
        board.undeleteBoard();
        updateBoardForRedis(board);
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
        return boardRedisRepository.getLikesCount(boardId);
    }

    // 이미지 파일 저장
    private void saveImageFile(List<ImageFileDto> images, Board board) {
        List<ImageFile> imageFiles = images.stream()
                .map(image -> imageFileRepository.save(image.toEntity(board))).collect(Collectors.toList());
        imageFiles.forEach(board::addImageFile);
    }

    private void updateBoardForRedis (Board board){
        try {
            boardRedisRepository.update(board);
        } catch (Exception e) {
            throw new RuntimeException("게시글 수정을 실패했습니다.");
        }
    }
}

