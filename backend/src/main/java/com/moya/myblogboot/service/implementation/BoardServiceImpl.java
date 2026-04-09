package com.moya.myblogboot.service.implementation;

import com.moya.myblogboot.domain.admin.Admin;
import com.moya.myblogboot.domain.board.*;
import com.moya.myblogboot.domain.category.Category;
import com.moya.myblogboot.domain.file.ImageFile;
import com.moya.myblogboot.domain.file.ImageFileDto;
import com.moya.myblogboot.dto.board.*;
import com.moya.myblogboot.exception.ErrorCode;
import com.moya.myblogboot.exception.custom.EntityNotFoundException;
import com.moya.myblogboot.exception.custom.UnauthorizedAccessException;
import com.moya.myblogboot.repository.AdminRepository;
import com.moya.myblogboot.repository.BoardRedisRepository;
import com.moya.myblogboot.repository.BoardRepository;
import com.moya.myblogboot.repository.ImageFileRepository;
import com.moya.myblogboot.service.BoardCacheService;
import com.moya.myblogboot.service.BoardService;
import com.moya.myblogboot.service.CategoryService;
import com.moya.myblogboot.service.FileUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Transactional(readOnly = true)
@Service
@RequiredArgsConstructor
public class BoardServiceImpl implements BoardService {

    private final CategoryService categoryService;
    private final AdminRepository adminRepository;
    private final BoardRepository boardRepository;
    private final ImageFileRepository imageFileRepository;
    private final BoardRedisRepository boardRedisRepository;
    private final FileUploadService fileUploadService;
    private final BoardCacheService boardCacheService;

    private static final int LIMIT = 8;

    @Override
    public BoardListResDto retrieveAll(int page) {
        PageRequest pageRequest = PageRequest.of(page, LIMIT, Sort.by(Sort.Direction.DESC, "createDate"));
        return convertToBoardListResDto(boardRepository.findAll(BoardStatus.VIEW, pageRequest));
    }

    @Override
    public BoardListResDto retrieveAllByCategory(String categoryName, int page) {
        PageRequest pageRequest = PageRequest.of(page, LIMIT, Sort.by(Sort.Direction.DESC, "createDate"));
        return convertToBoardListResDto(boardRepository.findAllByCategoryName(categoryName, pageRequest));
    }

    @Override
    public BoardListResDto retrieveAllBySearched(SearchType searchType, String searchContents, int page) {
        PageRequest pageRequest = PageRequest.of(page, LIMIT, Sort.by(Sort.Direction.DESC, "createDate"));
        return convertToBoardListResDto(boardRepository.findBySearchType(pageRequest, searchType, searchContents));
    }

    @Override
    public BoardListResDto retrieveAllDeleted(int page) {
        PageRequest pageRequest = PageRequest.of(page, LIMIT, Sort.by(Sort.Direction.DESC, "deleteDate"));
        return convertToBoardListResDto(boardRepository.findByDeletionStatus(pageRequest));
    }

    @Override
    public BoardDetailResDto getBoardDetail(Long boardId) {
        return BoardDetailResDto.builder()
                .boardForRedis(boardCacheService.getBoardFromCache(boardId))
                .build();
    }

    @Override
    public BoardDetailResDto getBoardDetailAndIncrementViews(Long boardId) {
        BoardForRedis boardForRedis = boardCacheService.getBoardFromCache(boardId);
        return BoardDetailResDto.builder()
                .boardForRedis(boardRedisRepository.incrementViews(boardForRedis))
                .build();
    }

    @Override
    @Transactional
    public Long write(BoardReqDto boardReqDto, Long adminId) {
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.MEMBER_NOT_FOUND));
        Category category = categoryService.retrieve(boardReqDto.getCategory());
        Board newBoard = boardReqDto.toEntity(category, admin);
        if (boardReqDto.getImages() != null && !boardReqDto.getImages().isEmpty()) {
            saveImageFile(boardReqDto.getImages(), newBoard);
        }
        Board result = boardRepository.save(newBoard);
        category.addBoard(result);
        return result.getId();
    }

    @Override
    @Transactional
    public Long edit(Long adminId, Long boardId, BoardReqDto modifiedDto) {
        Board board = findById(boardId);
        verifyBoardAccessAuthorization(board.getAdmin().getId(), adminId);
        Category modifiedCategory = categoryService.retrieve(modifiedDto.getCategory());
        board.updateBoard(modifiedCategory, modifiedDto.getTitle(), modifiedDto.getContent());
        boardCacheService.updateBoard(boardCacheService.getBoardFromCache(board.getId()), board);
        return boardId;
    }

    @Override
    @Transactional
    public void delete(Long boardId, Long adminId) {
        Board board = findById(boardId);
        verifyBoardAccessAuthorization(board.getAdmin().getId(), adminId);
        board.deleteBoard();
        boardCacheService.updateBoard(boardCacheService.getBoardFromCache(board.getId()), board);
    }

    @Override
    @Transactional
    public void undelete(Long boardId, Long adminId) {
        Board board = findById(boardId);
        verifyBoardAccessAuthorization(board.getAdmin().getId(), adminId);
        board.undeleteBoard();
        boardCacheService.updateBoard(boardCacheService.getBoardFromCache(board.getId()), board);
    }

    @Override
    @Transactional
    public void deletePermanently(LocalDateTime thresholdDate) {
        boardRepository.findByDeleteDate(thresholdDate).forEach(this::deleteBoards);
    }

    @Override
    @Transactional
    public void deletePermanently(Long boardId) {
        deleteBoards(findById(boardId));
    }

    @Override
    public Board findById(Long boardId) {
        return boardRepository.findById(boardId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.BOARD_NOT_FOUND));
    }

    private BoardListResDto convertToBoardListResDto(Page<Board> boards) {
        List<BoardResDto> resultList = boards.stream()
                .map(BoardResDto::of)
                .toList();
        return BoardListResDto.builder()
                .list(resultList)
                .totalPage(boards.getTotalPages())
                .build();
    }

    private void verifyBoardAccessAuthorization(Long boardAdminId, Long adminId) {
        if (!boardAdminId.equals(adminId))
            throw new UnauthorizedAccessException(ErrorCode.BOARD_ACCESS_DENIED);
    }

    private void deleteBoards(Board board) {
        BoardForRedis boardForRedis = boardCacheService.getBoardFromCache(board.getId());
        fileUploadService.deleteFiles(board.getImageFiles());
        boardCacheService.deleteBoard(boardForRedis);
        boardRepository.delete(board);
    }

    private void saveImageFile(List<ImageFileDto> images, Board board) {
        List<ImageFile> imageFiles = images.stream()
                .map(image -> imageFileRepository.save(image.toEntity(board)))
                .collect(Collectors.toList());
        imageFiles.forEach(board::addImageFile);
    }
}
