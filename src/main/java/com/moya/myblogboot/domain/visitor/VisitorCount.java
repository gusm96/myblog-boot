package com.moya.myblogboot.domain.visitor;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class VisitorCount {
    @Id // 날짜를 기준으로 구분하기에 date 컬럼을 PK로 설정
    private LocalDate date;
    private Long totalVisitors;
    private Long todayVisitors;

    public static VisitorCount of(LocalDate date, Long todayVisitors, Long totalVisitors) {
        return VisitorCount.builder()
                .date(date)
                .todayVisitors(todayVisitors)
                .totalVisitors(totalVisitors)
                .build();
    }
}
