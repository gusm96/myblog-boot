package com.moya.myblogboot.repository;

import java.time.LocalDateTime;

public interface BoardQuerydslRepository {

    void deleteWithinPeriod(LocalDateTime deleteDate);
}
