package com.moya.myblogboot.domain.comment;

import com.moya.myblogboot.domain.board.Board;
import com.moya.myblogboot.domain.board.ModificationStatus;
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

    @Column(nullable = false, length = 10)
    private String nickname;       // 작성자 닉네임 (어드민: "[관리자]")

    @Column(nullable = false, length = 4)
    private String discriminator;  // 4자리 식별번호 (어드민: "0000")

    @Column(nullable = false)
    private String password;       // BCrypt 해시 (어드민 댓글은 빈 문자열)

    @Column(nullable = false)
    private Boolean isAdmin;       // 어드민 작성 여부

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "board_id")
    private Board board;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "parent_id")
    private Comment parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<Comment> child = new ArrayList<>();

    @Builder
    public Comment(String comment, String nickname, String discriminator,
                   String password, Boolean isAdmin, Board board) {
        this.comment = comment;
        this.nickname = nickname;
        this.discriminator = discriminator;
        this.password = password;
        this.isAdmin = isAdmin;
        this.write_date = LocalDateTime.now();
        this.modificationStatus = ModificationStatus.NOT_MODIFIED;
        this.board = board;
    }

    public void addParentComment(Comment parent) {
        this.parent = parent;
    }

    public void addChildComment(Comment child) {
        this.child.add(child);
        child.addParentComment(this);
    }

    public void updateComment(String comment) {
        this.comment = comment;
        this.modificationStatus = ModificationStatus.MODIFIED;
    }
}
