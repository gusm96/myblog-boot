package com.moya.myblogboot.domain.tag;

import com.moya.myblogboot.domain.post.Post;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "post_tag",
        uniqueConstraints = @UniqueConstraint(name = "uk_post_tag", columnNames = {"post_id", "tag_id"})
)
@Getter
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_tag_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @BatchSize(size = 50)
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "is_primary", nullable = false)
    private boolean primary = false;

    @Column(name = "create_date", nullable = false, updatable = false)
    private LocalDateTime createDate;

    @Builder
    public PostTag(Post post, Tag tag, int sortOrder) {
        this.post = post;
        this.tag = tag;
        this.sortOrder = sortOrder;
        this.primary = false;
        this.createDate = LocalDateTime.now();
    }

    public void changeSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }
}
