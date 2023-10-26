package com.moya.myblogboot.repository;

import com.moya.myblogboot.domain.member.Role;

import java.util.Optional;

public interface RoleRepositoryInf {

    int save(Role role);

    Optional<Role> findOne(String roleName);
}
