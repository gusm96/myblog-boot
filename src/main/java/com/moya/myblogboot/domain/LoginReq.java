package com.moya.myblogboot.domain;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoginReq {
    private String admin_name;
    private String admin_pw;
}
