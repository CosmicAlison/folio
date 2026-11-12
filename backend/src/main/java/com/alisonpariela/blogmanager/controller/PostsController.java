package com.alisonpariela.blogmanager.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alisonpariela.blogmanager.model.Post;
import com.alisonpariela.blogmanager.service.PostService;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;

import org.springframework.data.domain.Sort;
import lombok.AllArgsConstructor;

@AllArgsConstructor
@RestController
@RequestMapping("/api/posts")
public class PostsController {
    private final PostService postService;

    @GetMapping
    public ResponseEntity<Page<Post>> getMyPosts(
        @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
        Pageable pageable
    ){
        Page<Post> posts = postService.getUserPosts(pageable);
        return ResponseEntity.ok(posts);
    }

    @PostMapping
    public ResponseEntity<Post> createPost(@Valid @RequestBody PostRequest request) {
        Post created = postService.createPost(request.getTitle(), request.getContents());
        return ResponseEntity.ok(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Post> updatePost(
            @PathVariable Long id,
            @Valid @RequestBody PostRequest request
    ) {
        Post updated = postService.updatePost(id, request.getTitle(), request.getContents());
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePost(@PathVariable Long id) {
        postService.deletePost(id);
        return ResponseEntity.noContent().build();
    }

    public static class PostRequest {
        @jakarta.validation.constraints.NotBlank
        @jakarta.validation.constraints.Size(max=200)
        private String title;
        @jakarta.validation.constraints.NotBlank
        @jakarta.validation.constraints.Size(max=50000)
        private String contents;

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getContents() { return contents; }
        public void setContents(String contents) { this.contents = contents; }
    }

}
