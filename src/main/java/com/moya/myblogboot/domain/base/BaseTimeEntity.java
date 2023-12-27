package com.moya.myblogboot.domain.base;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@MappedSuperclass
public abstract class BaseTimeEntity {

    @Column(updatable = false)
    private LocalDateTime createDate;

    private LocalDateTime updateDate;

    private LocalDateTime deleteDate;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createDate = now;
        updateDate = now;
    }

    @PreUpdate
    public void preUpdate() {
        updateDate = LocalDateTime.now();
    }

    public void delete(){
        this.deleteDate = LocalDateTime.now();
}
    public void undelete() {
        this.deleteDate = null;
    }
}
