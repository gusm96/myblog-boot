package com.moya.myblogboot.domain.admin;

import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AdminResDto {
    private Long id;
    private String admin_name;
    private String nickname;

    @Builder
    public AdminResDto(Admin admin) {
        this.id = admin.getId();
        this.admin_name = admin.getAdmin_name();
        this.nickname = admin.getNickname();
    }
}

