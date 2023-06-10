package com.moya.myblogboot.domain;

import jakarta.persistence.*;
import jdk.jfr.Name;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Admin {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "admin_id")
    private Long id;

    @Column(nullable = false)
    private String admin_name;
    @Column(nullable = false)
    private String admin_pw;
    @Column(nullable = false)
    private String nickname;

    @OneToMany(mappedBy = "admin")
    private List<Board> boards = new ArrayList<>();
    @OneToMany(mappedBy = "admin")
    private List<Reply> replies = new ArrayList<>();

    @Builder
    public Admin(String admin_name, String admin_pw, String nickname){
        this.admin_name = admin_name;
        this.admin_pw = admin_pw;
        this.nickname = nickname;
    }

}
