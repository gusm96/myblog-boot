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
        Page<Board> boards = boardRepository.findAllByCategoryName(categoryName, pageRequest);
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
                        -> BoardResDto.of(board))
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

    // 게시글 상세 조회
    @Override
    public BoardResDtoV2 retrieveBoardDetail(Long boardId) {
        // Redis Store에서 데이터 조회
        Optional<BoardForRedis> boardForRedis = boardRedisRepository.findOne(boardId);
        // Redis Store에 데이트 없으면 DB에서 조회
        if (boardForRedis.isEmpty()) {
            // DB에서 조회
            Board board = retrieveBoardById(boardId);
            // Redis Store에 Dada Set();
            BoardForRedis saveBoard = boardRedisRepository.save(board);
            // 조회수 증가 후 응답
            return BoardResDtoV2.builder().boardForRedis(boardRedisRepository.incrementViews(saveBoard)).build();
        }else {
            // 조회수 증가 후 응답
            return BoardResDtoV2.builder().boardForRedis(boardRedisRepository.incrementViews(boardForRedis.get())).build();
        }
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

    // 게시글 삭제 (영구 삭제 X)
    @Override
    @Transactional
    public boolean deleteBoard(Long boardId, Long memberId){
        // Entity 조회
        Board board = retrieveBoardById(boardId);
        if (board.getMember().getId().equals(memberId)) {
            board.deleteBoard(); // 15일 이후 자동 삭제
            updateBoardForRedis(board); // 현재 게시글 상태 Redis Store에 Update
            return true;
        }else {
            throw new UnauthorizedAccessException("권한이 없습니다.");
        }
    }

    // 게시글 삭제 취소
    @Override
    @Transactional
    public void undeleteBoard(Long boardId) {
        Board board = retrieveBoardById(boardId);
        board.undeleteBoard();
        updateBoardForRedis(board);
    }

    // 보관 기간이 만료된 게시글 영구 삭제
    @Override
    @Transactional
    public void deletePermanently(LocalDateTime thresholdDate) {
        List<Board> boards = boardRepository.findByDeleteDate(thresholdDate);
        for (Board b : boards) {
            // 이미지 파일 찾아서 S3에서 삭제
            b.getImageFiles().stream().forEach(imageFile ->
                    fileUploadService.deleteImageFile(imageFile.getFileName())
            );
            try {
                boardRepository.delete(b);
                boardRedisRepository.delete(b.getId());
            } catch (Exception e) {
                throw new RuntimeException("게시글 삭제를 실패했습니다.");
            }
        }
    }
    // 지정 게시글 영구 삭제
    @Override
    @Transactional
    public void deletePermanently(Long boardId) {
        Board board = retrieveBoardById(boardId);
            // 이미지 파일 찾아서 S3에서 삭제
            try {
                board.getImageFiles().stream().forEach(imageFile
                        -> fileUploadService.deleteImageFile(imageFile.getFileName()));
                boardRepository.delete(board);
                boardRedisRepository.delete(boardId);
            } catch (Exception e) {
                throw new RuntimeException("게시글 삭제를 실패했습니다.");
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

