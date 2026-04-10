package com.moya.myblogboot.domain.category;

import com.moya.myblogboot.domain.post.Post;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    private Long id;
    @Column(unique = true)
    private String name;
    @OneToMany(mappedBy = "category")
    private List<Post> posts = new ArrayList<>();

    @Builder
    public Category(String name) {
        this.name = name;
    }

    public void editCategory(String name){
        this.name = name;
    }
    // Category 삭제 시에 Post의 Category 정보를 null로 설정
    @PreRemove
    private void removePosts() {
        for (Post post : posts) {
            post.removeCategory();
        }
    }
    public void addPost(Post post) {
        this.posts.add(post);
    }
}
