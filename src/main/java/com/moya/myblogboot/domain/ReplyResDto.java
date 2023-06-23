package com.moya.myblogboot.domain;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ReplyResDto {
    private String writer;
    private String comment;
    private LocalDateTime write_date;
    private ModificationStatus modificationStatus;
    private List<ReplyResDto> child = new ArrayList<>();
}
