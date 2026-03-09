package com.moya.myblogboot.dto.visitor;


import static com.moya.myblogboot.domain.keys.RedisKey.*;
import lombok.*;

import java.util.HashMap;
import java.util.Map;


@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class VisitorCountDto {
    private Long total;
    private Long today;
    private Long yesterday;

    @Builder
    public VisitorCountDto (Long today, Long yesterday, Long total){
        this.today = today;
        this.yesterday = yesterday;
        this.total = total;
    }

    public Map<String, Long> toMap(){
        Map<String ,Long> map = new HashMap<>();
        map.put(TOTAL_COUNT_KEY, total);
        map.put(TODAY_COUNT_KEY, today);
        map.put(YESTERDAY_COUNT_KEY, yesterday);
        return map;
    }

    public void increment(){
        this.total++;
        this.today++;
    }
}
