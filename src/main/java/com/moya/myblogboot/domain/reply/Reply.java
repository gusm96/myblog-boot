package com.moya.myblogboot.domain.reply;

import com.moya.myblogboot.domain.board.ModificationStatus;
import com.moya.myblogboot.domain.board.Board;
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
public class Reply {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reply_id")
    private Long id;

    @Column(nullable = false)
    private String writer;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String comment;

    private LocalDateTime write_date;

    @Enumerated(EnumType.STRING)
    private ModificationStatus modificationStatus;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "board_id")
    private Board board;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "parent_id")
    private Reply parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.REMOVE , orphanRemoval = true)
    private List<Reply> child = new ArrayList<>();

    // 댓글 작성
    @Builder
    public Reply(String writer, String password, String comment, Board board) {
        this.writer = writer;
        this.password = password;
        this.comment = comment;
        this.write_date = LocalDateTime.now();
        this.modificationStatus = ModificationStatus.NOT_MODIFIED;
        this.board = board;
    }

    public void addParentReply(Reply parent) {
        this.parent = parent;
    }
    // 대댓글 작성
    public void addChildReply(Reply child) {
        this.child.add(child);
        child.addParentReply(this);
    }
    // 수정 메서드
    public void updateReply(String comment) {
        this.comment = comment;
        this.modificationStatus = ModificationStatus.MODIFIED;
    }
}
