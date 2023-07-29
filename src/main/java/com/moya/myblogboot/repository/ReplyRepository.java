package com.moya.myblogboot.repository;

import com.moya.myblogboot.domain.reply.Reply;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

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
    public Optional<Reply> findOne(Long replyId) {
        try{
            Reply result = em.find(Reply.class, replyId);
            return Optional.ofNullable(result);
        }catch (NoResultException e){
            return Optional.empty();
        }
    }
    @Override
    public List<Reply> replyList(Long boardId) {
        List<Reply> result = em.createQuery("select r from Reply r where r.board.id =: boardId order by r.write_date desc ", Reply.class)
                .setParameter("boardId", boardId)
                .getResultList();
        return result;
    }

    @Override
    public void removeReply(Long replyId) {
        Reply reply = em.find(Reply.class, replyId);
        if (reply != null) {
            em.remove(reply);
        }
    }
}
