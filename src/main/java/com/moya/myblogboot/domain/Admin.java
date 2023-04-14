package com.moya.myblogboot.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jdk.jfr.Name;
import lombok.*;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Table(name = "admin")
public class Admin {
    @Id
    @Column(name = "aidx")
    private Long idx;
    @Column(name = "admin_id")
    private String id;
    @Column(name = "admin_pw")
    private String pw;
    @Column(name = "admin_name")
    private String name;
}
