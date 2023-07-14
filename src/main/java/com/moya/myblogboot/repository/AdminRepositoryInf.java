package com.moya.myblogboot.repository;

import com.moya.myblogboot.domain.admin.Admin;

import java.util.Optional;

public interface AdminRepositoryInf {
    Optional<Admin> findById(String adminName);

    Long save(Admin admin);
}
