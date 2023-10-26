package com.moya.myblogboot.domain.token;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "token")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String token;

    @Column(nullable = false)
    private String username;

    @Column(name = "token_role", nullable = false)
    private String tokenRole;

    @Builder
    public RefreshToken(String token, String username, String tokenRole) {
        this.token = token;
        this.username = username;
        this.tokenRole = tokenRole;
    }
}
