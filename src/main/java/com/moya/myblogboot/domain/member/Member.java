package com.moya.myblogboot.domain.member;

import com.moya.myblogboot.domain.board.BoardLike;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long id;
    private String username;
    private String password;
    private String nickname;
    @Enumerated(EnumType.STRING)
    private Role role;

    /*@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_like_id")
    private BoardLike boardLike;*/

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
    /*public void setBoardLike (BoardLike boardLike){
        this.setBoardLike(boardLike);
    }*/
}
