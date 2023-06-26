package com.moya.myblogboot.domain;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReplyResDto {
    private Long id;
    private Long boardId;
    private Long parentId;
    private String writer;
    private String comment;
    private LocalDateTime write_date;
    private ModificationStatus modificationStatus;

    @Builder
    public ReplyResDto(Reply reply) {
        this.id = reply.getId();
        this.boardId = reply.getBoard().getId();
        if(reply.getParent() != null){
            this.parentId = reply.getParent().getId();
        }
        this.writer = reply.getWriter();
        this.comment = reply.getComment();
        this.write_date = reply.getWrite_date();
        this.modificationStatus = reply.getModificationStatus();
    }

    public static ReplyResDto of(Reply reply) {
        return ReplyResDto.builder()
                .reply(reply)
                .build();
    }

}
