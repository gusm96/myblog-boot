package com.moya.myblogboot.repository;


import com.moya.myblogboot.domain.guest.Guest;

import java.util.Optional;

public interface GuestRepositoryInf {
    Long save(Guest guest);
    Optional<Guest> findByName(String username);
}
