package com.moya.myblogboot.controller;

import com.moya.myblogboot.service.PostService;
import com.moya.myblogboot.utils.PrincipalUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequiredArgsConstructor
public class DeletedPostController {

    private final PostService postService;

    @GetMapping("/api/v1/deleted-posts")
    public ResponseEntity<?> getDeletedPosts(@RequestParam(name = "p", defaultValue = "1") int page) {
        return ResponseEntity.ok().body(postService.retrieveAllDeleted(page - 1));
    }

    @PutMapping("/api/v1/deleted-posts/{postId}")
    public ResponseEntity<?> cancelDeletedPost(@PathVariable("postId") Long postId,
                                               Principal principal) {
        Long memberId = PrincipalUtil.getMemberId(principal);
        postService.undelete(postId, memberId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @DeleteMapping("/api/v1/deleted-posts/{postId}")
    public ResponseEntity<?> deletePostPermanently(@PathVariable("postId") Long postId) {
        postService.deletePermanently(postId);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
