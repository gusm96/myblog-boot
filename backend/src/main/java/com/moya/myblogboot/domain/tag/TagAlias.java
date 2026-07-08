package com.moya.myblogboot.domain.tag;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "tag_alias")
@Getter
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TagAlias {

    @Id
    @Column(name = "from_slug", length = 60)
    private String fromSlug;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_tag_id", nullable = false)
    private Tag toTag;

    @Column(name = "merged_at", nullable = false)
    private LocalDateTime mergedAt;

    @Builder
    public TagAlias(String fromSlug, Tag toTag) {
        this.fromSlug = fromSlug;
        this.toTag = toTag;
        this.mergedAt = LocalDateTime.now();
    }
}
