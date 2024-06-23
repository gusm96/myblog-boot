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
import org.springframework.scheduling.annotation.Async;
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
    public BoardListResDto retrieveAll(int page) {
        PageRequest pageRequest = PageRequest.of(page, LIMIT, Sort.by(Sort.Direction.DESC, "createDate"));
        Page<Board> boards = boardRepository.findAll(BoardStatus.VIEW, pageRequest);
        return convertToBoardListResDto(boards);
    }

    // 카테고리별 게시글 리스트
    @Override
    public BoardListResDto retrieveAllByCategory(String categoryName, int page) {
        PageRequest pageRequest = PageRequest.of(page, LIMIT, Sort.by(Sort.Direction.DESC, "createDate"));
        Page<Board> boards = boardRepository.findAllByCategoryName(categoryName, pageRequest);
        return convertToBoardListResDto(boards);
    }

    // 검색한 게시글 리스트
    @Override
    public BoardListResDto retrieveAllBySearched(SearchType searchType, String searchContents, int page) {
        PageRequest pageRequest = PageRequest.of(page, LIMIT, Sort.by(Sort.Direction.DESC, "createDate"));
        // 검색어 + 페이지 게시글 조회
        Page<Board> boards = boardRepository.findBySearchType(pageRequest, searchType, searchContents);
        return convertToBoardListResDto(boards);
    }

    // 삭제 예정(휴지통) 게시글 리스트
    @Override
    public BoardListResDto retrieveAllDeleted(int page) {
        PageRequest pageRequest = PageRequest.of(page, LIMIT, Sort.by(Sort.Direction.DESC, "deleteDate"));
        Page<Board> boards = boardRepository.findByDeletionStatus(pageRequest);
        return convertToBoardListResDto(boards);
    }

    // 게시글 상세 조회
    @Override
    public BoardDetailResDto retrieveDto(Long boardId) {
        BoardForRedis boardForRedis = retrieveBoardInRedisStore(boardId);
        return BoardDetailResDto.builder().boardForRedis(boardForRedis).build();
    }

    // 게시글 조회 및 조회수 증가
    @Override
    public BoardDetailResDto retrieveAndIncrementViewsDto(Long boardId) {
        BoardForRedis boardForRedis = retrieveBoardInRedisStore(boardId);
        return BoardDetailResDto.builder().boardForRedis(incrementViews(boardForRedis)).build();
    }

    // 게시글 업로드
    @Override
    @Transactional
    public Long write(BoardReqDto boardReqDto, Long memberId) {
        Member member = authService.retrieve(memberId);
        Category category = categoryService.retrieve(boardReqDto.getCategory());
        Board newBoard = boardReqDto.toEntity(category, member);
        try {
            if (boardReqDto.getImages() != null && boardReqDto.getImages().size() > 0) {
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

    // 게시글 수정
    @Override
    @Transactional
    public Long edit(Long memberId, Long boardId, BoardReqDto modifiedDto) {
        // Entity 조회
        Board board = retrieve(boardId);
        // 게시글 수정/삭제 권한 검사.
        verifyBoardAccessAuthorization(board.getMember().getId(), memberId);
        Category modifiedCategory = categoryService.retrieve(modifiedDto.getCategory());
        board.updateBoard(modifiedCategory, modifiedDto.getTitle(), modifiedDto.getContent()); // 변경감지
        // 변경 된 내용 Redis Store에 업데이트
        updateBoardForRedis(board);
        return boardId;
    }

    // 게시글 삭제 (영구 삭제 X)
    @Override
    @Transactional
    public void delete(Long boardId, Long memberId) {
        // Entity 조회
        Board board = retrieve(boardId);
        // 게시글 수정/삭제 권한 검사.
        verifyBoardAccessAuthorization(board.getMember().getId(), memberId);
        // 삭제 요청일 갱신
        board.deleteBoard();
        // 현재 게시글 상태 Redis Store에 Update
        updateBoardForRedis(board);
    }

    // 게시글 삭제 취소
    @Override
    @Transactional
    public void undelete(Long boardId, Long memberId) {
        // DB에서 게시글 조회
        Board board = retrieve(boardId);
        // 게시글 수정/삭제 권한 검사.
        verifyBoardAccessAuthorization(board.getMember().getId(), memberId);
        // DeleteDate, BoardStatus 업데이트
        board.undeleteBoard();
        // Redis Store에 저장된 Data 업데이트
        updateBoardForRedis(board);
    }

    // 보관 기간이 만료된 게시글 영구 삭제
    @Override
    @Transactional
    public void deletePermanently(LocalDateTime thresholdDate) {
        List<Board> boards = boardRepository.findByDeleteDate(thresholdDate);
        boards.stream().forEach(Board::deleteBoard);
    }

    // 지정 게시글 영구 삭제
    @Override
    @Transactional
    public void deletePermanently(Long boardId) {
        Board board = retrieve(boardId);
        deleteBoards(board);
    }

    // 게시글 Entity 조회
    @Override
    public Board retrieve(Long boardId) {
        try {
            return boardRepository.findById(boardId).orElseThrow(
                    () -> new EntityNotFoundException("해당 게시글이 존재하지 않습니다.")
            );
        } catch (InvalidDataAccessApiUsageException e) {
            e.printStackTrace();
            throw new RuntimeException("게시글 조회중 오류 발생.");
        }
    }

    private BoardListResDto convertToBoardListResDto(Page<Board> boards) {
        // 조회한 Board Entity List를 DTO 객체로 변환.
        List<BoardResDto> resultList = boards.stream().map(board
                        -> BoardResDto.of(board))
                .toList();
        // 화면에 보여질 List와 개시글 총 개수 반환.
        return BoardListResDto.builder()
                .list(resultList)
                .totalPage(boards.getTotalPages())
                .build();
    }

    // 게시글 수정/삭제 권한 검사.
    private void verifyBoardAccessAuthorization(Long boardsMemberId, Long memberId) {
        if (!boardsMemberId.equals(memberId))
            throw new UnauthorizedAccessException("게시글 수정 권한이 없습니다.");
    }

    // 게시글 영구 삭제
    private void deleteBoards(Board board) {
        // 이미지 파일 찾아서 S3에서 삭제
        fileUploadService.deleteFiles(board.getImageFiles());
        // Redis Store에 저장된 Data Delete
        deleteBoardForRedis(board.getId());
        try {
            // DB에 저장된 Data Delete
            boardRepository.delete(board);
        } catch (Exception e) {
            throw new RuntimeException("게시글 삭제를 실패했습니다.");
        }
    }

    // Board Entity 조회 후 Redis Store에 저장.
    private BoardForRedis retrieveBoardAndSetRedisStore(Long boardId) {
        Board board = retrieve(boardId);
        return boardRedisRepository.save(board);
    }

    // 조회 수 증가.
    private BoardForRedis incrementViews(BoardForRedis boardForRedis) {
        return boardRedisRepository.incrementViews(boardForRedis);
    }

    // Redis Store에서 데이터 조회
    @Override
    public synchronized BoardForRedis retrieveBoardInRedisStore(Long boardId) {
        Optional<BoardForRedis> boardForRedis = boardRedisRepository.findOne(boardId);
        if (boardForRedis.isEmpty()) {
            // DB에서 Board 조회 후 Redis store에 저장.
            return retrieveBoardAndSetRedisStore(boardId);
        }
        return boardForRedis.get();
    }

    // 이미지 파일 저장
    private void saveImageFile(List<ImageFileDto> images, Board board) {
        List<ImageFile> imageFiles = images.stream()
                .map(image -> imageFileRepository.save(image.toEntity(board))).collect(Collectors.toList());
        imageFiles.forEach(board::addImageFile);
    }

    @Async
    protected void updateBoardForRedis(Board board) {
        BoardForRedis boardForRedis = retrieveBoardInRedisStore(board.getId());
        boardForRedis.update(board);
        try {
            boardRedisRepository.update(boardForRedis);
        } catch (Exception e) {
            throw new RuntimeException("Failed to update board from Redis store");
        }
    }

    @Async
    protected void deleteBoardForRedis(Long boardId) {
        BoardForRedis boardForRedis = retrieveBoardInRedisStore(boardId);
        try {
            boardRedisRepository.delete(boardForRedis);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to delete board from Redis store");
        }
    }


}

