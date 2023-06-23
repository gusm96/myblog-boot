package com.moya.myblogboot.repository;

import com.moya.myblogboot.domain.Reply;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ReplyRepository implements ReplyRepositoryInf {

    private final EntityManager em;


    @Override
    public Long write(Reply reply) {
        em.persist(reply);
        return reply.getId();
    }
    @Override
    public Reply findOne(Long replyId) {
        Reply result = em.find(Reply.class, replyId);
        return result;
    }

    @Override
    public List<Reply> replyList(Long boardId) {
        List<Reply> result = em.createQuery("select r from Reply r where r.board.id =: boardId order by r.write_date desc ", Reply.class)
                .setParameter("boardId", boardId)
                .getResultList();
        return result;
    }
}
