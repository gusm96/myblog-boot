package com.moya.myblogboot.domain.post;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "post_slug_history",
        indexes = {
                @Index(name = "uk_slug_history_old_slug", columnList = "old_slug", unique = true),
                @Index(name = "ix_slug_history_post_id", columnList = "post_id")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostSlugHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_slug_history_post"))
    private Post post;

    @Column(name = "old_slug", nullable = false, length = 255, unique = true)
    private String oldSlug;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    public PostSlugHistory(Post post, String oldSlug) {
        this.post = post;
        this.oldSlug = oldSlug;
        this.changedAt = LocalDateTime.now();
    }
}
