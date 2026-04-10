package com.moya.myblogboot.service;

public interface PostLikeService {

    Long addLikes(Long postId);

    Long cancelLikes(Long postId);
}
