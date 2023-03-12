package com.moya.myblogboot.controller;

import com.moya.myblogboot.service.BoardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class HomeController {
    @Autowired
    BoardService service;
    @GetMapping("/home")
    public String getHome(@RequestParam(name = "post") int bidx, Model model){
//        model.addAttribute("board", service.getBoard(bidx));
        return "home";
    }
}
