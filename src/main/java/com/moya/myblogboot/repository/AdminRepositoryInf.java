package com.moya.myblogboot.repository;

import com.moya.myblogboot.domain.Admin;

import java.util.Optional;

public interface AdminRepositoryInf {
    Optional<Admin> findById(String id);
}
