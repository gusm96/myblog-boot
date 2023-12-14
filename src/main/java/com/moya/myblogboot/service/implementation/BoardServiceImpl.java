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
import com.querydsl.core.QueryResults;
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
    @Override
    public BoardListResDto retrieveBoardListBySearch (SearchType searchType, String searchContents, int page) {
        // 검색어 + 페이지 게시글 조회
        QueryResults<Board> boards = boardRepository.findBySearchType(pagination(page), LIMIT, searchType, searchContents);
        // DTO 객체로 변환
        List<BoardResDto> resultList = boards.getResults().stream().map(board
                        -> BoardResDto.of(board, getBoardLikeCount(board.getId())))
                .toList();
        return BoardListResDto.builder()
                .list(resultList)
                .totalPage(pageCount(boards.getTotal()))
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

    // 게시글 상세
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
        return board.getId();
    }

    // 게시글 삭제
    @Override
    @Transactional
    public boolean deleteBoard(Long boardId, Long memberId){
        // Entity 조회
        Board board = retrieveBoardById(boardId);
        if (board.getMember().getId().equals(memberId)) {
            board.setDeleteDate(); // 15일 이후 자동 삭제
            return true;
        }else {
            throw new UnauthorizedAccessException("권한이 없습니다.");
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
        return boardRedisRepository.getLikesCount(boardId);
    }

    // 이미지 파일 저장
    private void saveImageFile(List<ImageFileDto> images, Board board) {
        List<ImageFile> imageFiles = images.stream()
                .map(image -> imageFileRepository.save(image.toEntity(board))).collect(Collectors.toList());
        imageFiles.forEach(board::addImageFile);
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
