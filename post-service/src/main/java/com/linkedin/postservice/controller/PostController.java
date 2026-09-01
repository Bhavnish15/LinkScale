package com.linkedin.postservice.controller;

import com.linkedin.postservice.entity.Comment;
import com.linkedin.postservice.entity.Post;
import com.linkedin.postservice.service.PostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/posts")
@Slf4j
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    // Create Post
    @PostMapping
    public ResponseEntity<Post> createPost(
            @RequestParam String authorId,
            @RequestParam String content,
            @RequestParam(required = false)MultipartFile image
    ) {
       return ResponseEntity.status(HttpStatus.CREATED).body(postService.createPost(authorId, content, image));
    }

    // Get Post
    @GetMapping("/{postId}")
    public ResponseEntity<Post> getPost(@PathVariable String postId) {
        return ResponseEntity.ok(postService.getPost(postId));
    }

    @GetMapping
    public ResponseEntity<List<Post>> getAllPost() {
        return ResponseEntity.ok(postService.getAllPost());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Post>> getAuthorPost(@RequestParam String authorId) {
        return ResponseEntity.ok(postService.getAuthorPost(authorId));
    }

    @PostMapping("/{postId}/{userId}/like")
    public ResponseEntity<String> LikePost(@RequestParam String postId, @RequestParam String userId){
        return ResponseEntity.ok(postService.LikePost(userId, postId));
    }

    @PostMapping("/{postId}/{userId}/{content}/comment")
    public ResponseEntity<Comment> addComment(@RequestParam String postId,
                                              @RequestParam String userId,
                                              @RequestParam String content){
        return ResponseEntity.ok(postService.addComment(postId, userId, content));
    }

    @DeleteMapping("/{postId}/{userId}/delete")
    public ResponseEntity<String> deletePost(@RequestParam String postId, @RequestParam String userId) {
        postService.deletePost(postId, userId);
        return ResponseEntity.ok("Post deleted");
    }

    @GetMapping("/{postId}/comments")
    public ResponseEntity<List<Comment>> getComment(@RequestParam String postId) {
        return ResponseEntity.ok(postService.getComment(postId));
    }
}
