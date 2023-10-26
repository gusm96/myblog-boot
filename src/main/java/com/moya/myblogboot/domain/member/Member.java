package com.moya.myblogboot.domain.member;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class Member {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String username;
    private String password;
    private String nickname;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id")
    private Role role;

    @Builder
    public Member (String username, String password, String nickname){
        this.username = username;
        this.password = password;
        this.nickname = nickname;
    }
    // 권한 부여
    public void addRole(Role role) {
        this.role = role;
    }
}
