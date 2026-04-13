package com.moya.myblogboot.domain.post;

import com.moya.myblogboot.domain.admin.Admin;
import com.moya.myblogboot.domain.base.BaseTimeEntity;
import com.moya.myblogboot.domain.category.Category;
import com.moya.myblogboot.domain.comment.Comment;
import com.moya.myblogboot.domain.file.ImageFile;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "post")
@Getter
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_id")
    private Long id;
    @Column(nullable = false)
    private String title;
    @Column(nullable = false, columnDefinition = "longtext")
    private String content;
    private Long views = 0L;
    private Long likes = 0L;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ImageFile> imageFiles = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "post_status")
    private PostStatus postStatus = PostStatus.VIEW;

    @Column(unique = true, length = 255)
    private String slug;

    @Column(name = "meta_description", length = 160)
    private String metaDescription;

    @Column(name = "meta_keywords", length = 255)
    private String metaKeywords;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id", nullable = false)
    private Admin admin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> comments = new ArrayList<>();

    @Builder
    public Post(String title, String content, Category category, Admin admin,
                String slug, String metaDescription, String metaKeywords, String thumbnailUrl) {
        this.title = title;
        this.content = content;
        this.category = category;
        this.admin = admin;
        this.slug = slug;
        this.metaDescription = metaDescription;
        this.metaKeywords = metaKeywords;
        this.thumbnailUrl = thumbnailUrl;
    }

    public void updatePost(Category category, String title, String content,
                           String slug, String metaDescription, String metaKeywords, String thumbnailUrl) {
        this.category = category;
        this.title = title;
        this.content = content;
        this.slug = slug;
        this.metaDescription = metaDescription;
        this.metaKeywords = metaKeywords;
        this.thumbnailUrl = thumbnailUrl;
    }

    public void updatePostStatus(PostStatus postStatus) {
        this.postStatus = postStatus;
    }

    public void addComment(Comment comment) {
        this.comments.add(comment);
    }

    public void removeComment(Comment comment) {
        this.comments.remove(comment);
    }

    public void addImageFile(ImageFile file) {
        this.imageFiles.add(file);
    }

    public void removeImageFile(ImageFile file) {
        this.imageFiles.remove(file);
    }

    public void removeCategory() {
        this.category = null;
    }

    public void updateViews(Long views) {
        this.views = views;
    }

    public void updateLikes(Long likes) {
        this.likes = likes;
    }

    public void deletePost() {
        this.delete();
        this.postStatus = PostStatus.HIDE;
    }

    public void undeletePost() {
        this.undelete();
        this.postStatus = PostStatus.VIEW;
    }
}
