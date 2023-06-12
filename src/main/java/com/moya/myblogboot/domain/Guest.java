package com.moya.myblogboot.domain;

import jakarta.persistence.*;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
public class Guest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "guest_id")
    private Long id;
    private String name;
    private String password;

    @OneToMany(mappedBy = "guest")
    private List<Reply> replies = new ArrayList<>();

    @OneToMany(mappedBy = "guest")
    private List<ReReply> reReplies = new ArrayList<>();
}
