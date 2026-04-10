package com.moya.myblogboot.domain.comment;

import com.moya.myblogboot.domain.base.BaseTimeEntity;
import com.moya.myblogboot.domain.post.ModificationStatus;
import com.moya.myblogboot.domain.post.Post;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.FetchType.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id")
    private Long id;

    @Column(nullable = false)
    private String comment;

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
    @JoinColumn(name = "post_id")
    private Post post;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "parent_id")
    private Comment parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<Comment> child = new ArrayList<>();

    @Builder
    public Comment(String comment, String nickname, String discriminator,
                   String password, Boolean isAdmin, Post post) {
        this.comment = comment;
        this.nickname = nickname;
        this.discriminator = discriminator;
        this.password = password;
        this.isAdmin = isAdmin;
        this.modificationStatus = ModificationStatus.NOT_MODIFIED;
        this.post = post;
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
