package com.moya.myblogboot.repository;

import com.moya.myblogboot.domain.Admin;

import java.util.Optional;

public interface AdminRepositoryInf {
    Long save(Admin admin);
    Optional<Admin> findById(String adminName);
}
