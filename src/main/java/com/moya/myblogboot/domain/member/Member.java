package com.moya.myblogboot.domain.member;

import com.moya.myblogboot.domain.abstration.BaseTimeEntity;
import com.moya.myblogboot.domain.board.BoardLike;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;


@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String username;
    private String password;
    private String nickname;
    @Enumerated(EnumType.STRING)
    private Role role;

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<BoardLike> boardLikes = new HashSet<>();
    @Builder
    public Member(String username, String password, String nickname) {
        this.username = username;
        this.password = password;
        this.nickname = nickname;
        this.role = Role.ROLE_NORMAL;
    }

    public void addRoleAdmin (){
        this.role = Role.ROLE_ADMIN;
    }

}
