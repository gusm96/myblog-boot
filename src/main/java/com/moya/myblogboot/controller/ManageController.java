package com.moya.myblogboot.controller;

import com.moya.myblogboot.domain.Board;
import com.moya.myblogboot.service.BoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class ManageController {

    private final BoardService service;

    @GetMapping("/manage")
    public String getManagement(){
        return "manage/manageHome";
    }
    @GetMapping("/manage/board/{type}")
    public String getManagementBoard(
            @PathVariable("type") String type,
            @RequestParam(name = "page", defaultValue = "1") int page,
            Model model) {
        model.addAttribute("list", "Board - list");
        return "manage/test";
    }

    @GetMapping("/newpost")
    public String getNewPostPage() {
        return "board/newpost";
    }

    @PostMapping("/newpost")
    public String postNewPostPage(Board board, Model model){
        model.addAttribute("result", service.newPost(board));
        return "board/newpostSuccess";
    }
}
