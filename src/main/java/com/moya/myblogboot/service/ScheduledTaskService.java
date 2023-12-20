package com.moya.myblogboot.service;

import com.moya.myblogboot.domain.board.Board;
import com.moya.myblogboot.domain.board.BoardForRedis;
import com.moya.myblogboot.domain.board.BoardLike;
import com.moya.myblogboot.domain.member.Member;
import com.moya.myblogboot.repository.BoardLikeRepository;
import com.moya.myblogboot.repository.BoardRedisRepository;
import com.moya.myblogboot.repository.BoardRepository;
import com.moya.myblogboot.repository.MemberRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledTaskService {
    private final BoardRepository boardRepository;
    private final BoardRedisRepository boardRedisRepository;
    private final MemberRepository memberRepository;
    private final BoardLikeRepository boardLikeRepository;
    private static final Long SECONDS_INT_15DAYS = 15L * 24L * 60L * 60L; // 15일

    @Scheduled(cron = "0 0 0 * * ?")// 매일 자정에 실행되도록 스케줄링
    @Transactional
    public void deleteExpiredBoards() {
        LocalDateTime thresholdDate = LocalDateTime.now().minusSeconds(SECONDS_INT_15DAYS);
        boardRepository.deleteWithinPeriod(thresholdDate);
        log.info("삭제 후 15일이 지난 게시글 영구삭제");
    }

    @Scheduled(fixedRate = 600000) // 10분마다 DB 갱싱 및 메모리 정리
    @Transactional
    public void updateBoards() {
        Set<Long> keys = getKeys("board:");
        for(Long key : keys) {
            log.info("Key = {}" + key);
            // 메모리에서 데이터 조회
            BoardForRedis boardForRedis = boardRedisRepository.findOne(key).get();
            if(boardForRedis != null){
                // 수정할 대상 게시글 엔터티 조회
                Board findBoard = getBoard(boardForRedis.getId());
                // 조회수 업데이트
                findBoard.updateViews(boardForRedis.getViews() + boardForRedis.getUpdateViews());
                // 좋아요 한 회원ID
                List<Long> membersId = boardForRedis.getLikes().stream().toList();
                // BoardLike Entity 생성
                saveBoardLikes(findBoard, membersId);
                // 메모리 데이터 삭제
                boardRedisRepository.delete(findBoard.getId());
            }
        }
        log.info("DataBase Update 완료");
    }

    private void saveBoardLikes(Board board, List<Long> membersId) {
        for (Long memberId : membersId) {
            Member member = getMember(memberId);
            BoardLike boardLike = BoardLike.builder().board(board).member(member).build();
            boardLikeRepository.save(boardLike);
        }
    }

    private Set<Long> getKeys(String key) {
        return boardRedisRepository.getKeysValues(key);
    }

    private Member getMember(Long memberId) {
        return memberRepository.findById(memberId).orElseThrow(
                () -> new EntityNotFoundException("회원이 존재하지 않습니다")
        );
    }

    private Board getBoard(Long boardId) {
        return boardRepository.findById(boardId).orElseThrow(
                () -> new EntityNotFoundException("게시글이 존재하지 않습니다.")
        );
    }
}