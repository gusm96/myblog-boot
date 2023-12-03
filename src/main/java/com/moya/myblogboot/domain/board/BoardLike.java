package com.moya.myblogboot.domain.board;

import com.moya.myblogboot.domain.member.Member;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;


@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class BoardLike {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "board_like_id")
    private Long id;
    private Long count;
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id")
    private Board board;
    @OneToMany
    @JoinTable(
            name = "board_like_member",
            joinColumns = @JoinColumn(name = "board_like_id"),
            inverseJoinColumns = @JoinColumn(name = "member_id")
    )
    private List<Member> members = new ArrayList<>();

    @Builder
    public BoardLike(Board board) {
        this.count = 0L;
        this.board = board;
    }
    public synchronized Long incrementLike(Member member){
        this.members.add(member);
        return ++this.count;
    }
    public Long decrementLike(){
        if(this.count - 1 <= 0) return 0L;
        return --this.count;
    }
}
