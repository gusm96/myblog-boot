package com.moya.myblogboot.domain.member;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String username;
    private String password;
    private String nickname;
    @Enumerated(EnumType.STRING)
    private Role role;

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
