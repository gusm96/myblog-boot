package com.moya.myblogboot.scheduler;

import com.moya.myblogboot.domain.board.Board;
import com.moya.myblogboot.dto.board.BoardForRedis;
import com.moya.myblogboot.repository.BoardRedisRepository;
import com.moya.myblogboot.service.BoardService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BoardScheduledTaskTest {

    @InjectMocks
    private BoardScheduledTask boardScheduledTask;

    @Mock
    private BoardRedisRepository boardRedisRepository;

    @Mock
    private BoardService boardService;

    @Test
    @DisplayName("캐시에 데이터가 있으면 DB에 동기화하고 캐시를 삭제한다")
    void updateFromRedisStoreToDB_success() {
        // given
        Set<Long> keys = Set.of(1L, 2L);
        given(boardRedisRepository.getKeys(anyString())).willReturn(keys);

        Board board1 = Board.builder().title("title1").content("content1").build();
        Board board2 = Board.builder().title("title2").content("content2").build();

        BoardForRedis cache1 = new BoardForRedis(board1);
        BoardForRedis cache2 = new BoardForRedis(board2);

        given(boardService.getBoardFromCache(1L)).willReturn(cache1);
        given(boardService.getBoardFromCache(2L)).willReturn(cache2);
        given(boardService.findById(any())).willReturn(board1, board2);

        // when
        boardScheduledTask.updateFromRedisStoreToDB();

        // then
        verify(boardRedisRepository, times(2)).delete(any(BoardForRedis.class));
        verify(boardService, times(2)).findById(any());
    }

    @Test
    @DisplayName("캐시에 데이터가 없으면 아무 작업도 하지 않는다")
    void updateFromRedisStoreToDB_emptyKeys() {
        // given
        given(boardRedisRepository.getKeys(anyString())).willReturn(Collections.emptySet());

        // when
        boardScheduledTask.updateFromRedisStoreToDB();

        // then
        verify(boardService, never()).getBoardFromCache(any());
        verify(boardService, never()).findById(any());
    }

    @Test
    @DisplayName("개별 게시글 동기화 실패 시 나머지 게시글은 계속 처리한다")
    void updateFromRedisStoreToDB_partialFailure() {
        // given
        Set<Long> keys = Set.of(1L, 2L, 3L);
        given(boardRedisRepository.getKeys(anyString())).willReturn(keys);

        Board board = Board.builder().title("title").content("content").build();
        BoardForRedis validCache = new BoardForRedis(board);

        // 1L: 성공, 2L: 예외 발생, 3L: 성공
        given(boardService.getBoardFromCache(1L)).willReturn(validCache);
        given(boardService.getBoardFromCache(2L)).willThrow(new RuntimeException("Redis connection error"));
        given(boardService.getBoardFromCache(3L)).willReturn(validCache);
        given(boardService.findById(any())).willReturn(board);

        // when
        boardScheduledTask.updateFromRedisStoreToDB();

        // then - 2L 실패해도 1L, 3L은 처리됨
        verify(boardService, times(3)).getBoardFromCache(any());
        verify(boardService, times(2)).findById(any());
    }

    @Test
    @DisplayName("getBoardFromCache가 null을 반환하면 해당 게시글을 건너뛴다")
    void updateFromRedisStoreToDB_nullCache() {
        // given
        Set<Long> keys = Set.of(1L);
        given(boardRedisRepository.getKeys(anyString())).willReturn(keys);
        given(boardService.getBoardFromCache(1L)).willReturn(null);

        // when
        boardScheduledTask.updateFromRedisStoreToDB();

        // then
        verify(boardService, never()).findById(any());
        verify(boardRedisRepository, never()).delete(any(BoardForRedis.class));
    }
}
