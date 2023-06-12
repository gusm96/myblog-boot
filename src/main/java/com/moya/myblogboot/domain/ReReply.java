package com.moya.myblogboot.domain;

import jakarta.persistence.*;
import static jakarta.persistence.FetchType.*;
import lombok.Getter;

@Entity
@Getter
public class ReReply {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String content;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "reply_id")
    private Reply reply;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "admin")
    private Admin admin;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "guest")
    private Guest guest;
}
