package com.moya.myblogboot.repository.implementation;

import com.moya.myblogboot.domain.comment.Comment;
import com.moya.myblogboot.repository.CommentRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CommentRepositoryImpl implements CommentRepository {

    private final EntityManager em;


    @Override
    public Long write(Comment comment) {
        em.persist(comment);
        return comment.getId();
    }
    @Override
    public Optional<Comment> findById(Long commentId) {
        try {
            Comment result = em.find(Comment.class, commentId);
            return Optional.ofNullable(result);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }
    @Override
    public List<Comment> commentList(Long boardId) {
        List<Comment> result = em.createQuery("select r from Comment r where r.board.id =: boardId order by r.write_date desc ", Comment.class)
                .setParameter("boardId", boardId)
                .getResultList();
        return result;
    }

    @Override
    public void removeComment(Comment comment) {
            em.remove(comment);
    }
}
