package com.moya.myblogboot.domain.tag;

import com.moya.myblogboot.domain.base.BaseTimeEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Tag extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tag_id")
    private Long id;

    @Column(nullable = false, length = 40)
    private String name;

    @Column(nullable = false, length = 60, unique = true, updatable = false)
    private String slug;

    @Column(name = "post_count", nullable = false)
    private int postCount = 0;

    @OneToMany(mappedBy = "tag", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PostTag> postTags = new ArrayList<>();

    @Builder
    public Tag(String name, String slug) {
        this.name = name;
        this.slug = slug;
    }

    public void rename(String name) {
        this.name = name;
    }

    public void incrementPostCount() {
        this.postCount++;
    }

    public void decrementPostCount() {
        this.postCount = Math.max(0, this.postCount - 1);
    }

    public void resetPostCount(int postCount) {
        this.postCount = Math.max(0, postCount);
    }
}
