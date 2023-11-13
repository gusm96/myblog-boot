package com.moya.myblogboot.domain.category;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    // 생성 메서드
    @Builder
    public Category(String name) {
        this.name = name;
    }

    // 수정 메서드
    public void editCategory(String name){
        this.name = name;
    }
}
