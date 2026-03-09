package com.moya.myblogboot.domain.comment;

import com.moya.myblogboot.domain.board.ModificationStatus;
import com.moya.myblogboot.domain.board.Board;
import com.moya.myblogboot.domain.member.Member;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.FetchType.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id")
    private Long id;

    @Column(nullable = false)
    private String comment;

    private LocalDateTime write_date;

    @Enumerated(EnumType.STRING)
    private ModificationStatus modificationStatus;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "board_id")
    private Board board;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "parent_id")
    private Comment parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<Comment> child = new ArrayList<>();

    // 댓글 작성
    @Builder
    public Comment(String comment, Member member , Board board) {
        this.comment = comment;
        this.write_date = LocalDateTime.now();
        this.modificationStatus = ModificationStatus.NOT_MODIFIED;
        this.member = member;
        this.board = board;
    }


    public void addParentComment(Comment parent) {
        this.parent = parent;
    }
    // 대댓글 작성
    public void addChildComment(Comment child) {
        this.child.add(child);
        child.addParentComment(this);
    }
    // 수정 메서드
    public void updateComment(String comment) {
        this.comment = comment;
        this.modificationStatus = ModificationStatus.MODIFIED;
    }
}
