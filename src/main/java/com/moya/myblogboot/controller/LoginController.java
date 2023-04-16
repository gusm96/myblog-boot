package com.moya.myblogboot.controller;

import com.moya.myblogboot.domain.Admin;
import com.moya.myblogboot.service.LoginService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class LoginController {

    private final LoginService service;
    @GetMapping("/login/admin")
    public String getAdminLoginPage(
            @RequestParam(value ="error",required = false) String error,
            @RequestParam(value = "exception", required = false) String exception,
            Model model) {
        model.addAttribute("error", error);
        model.addAttribute("exception", exception);
        return "admin/loginForm";
    }


    @GetMapping("/logout/admin")
    public String adminLogout(){
        // Token을 만료 시킨다.


        return "redirect:/";
    }
}
