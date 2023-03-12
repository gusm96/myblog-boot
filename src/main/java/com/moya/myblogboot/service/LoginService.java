package com.moya.myblogboot.service;

import com.moya.myblogboot.domain.AdminDTO;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

@Service
public class LoginService {

    public String adminLogin(HttpSession session, AdminDTO admin){
        String result = "";

        // DB 확인 후 200 = manage page / 404 = exception

        return result;
    }
}
