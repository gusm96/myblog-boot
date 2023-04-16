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
    @Setter
    @Column(name = "admin_id")
    private String id;
    @Setter
    @Column(name = "admin_pw")
    private String pw;
    @Column(name = "admin_name")
    private String name;

    public Admin(String id, String pw){
        this.id = id;
        this.pw = pw;
    }
}
