package com.moya.myblogboot.controller;

import com.moya.myblogboot.domain.BoardDTO;
import com.moya.myblogboot.service.BoardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/manage")
public class ManageController {

    @Autowired
    BoardService service;

    @GetMapping("/{type}")
    public String getManagement(
            @PathVariable("type") String type,
            @RequestParam(name = "page", defaultValue = "1") int page,
            Model model) {
        model.addAttribute("list", "Board - list");
        return "manage/manageHome";
    }

    @GetMapping("/newpost")
    public String getNewPostPage() {
        return "board/newpost";
    }

    @PostMapping("/newpost")
    public String postNewPostPage(BoardDTO board){
        return service.newPost(board);
    }
}
