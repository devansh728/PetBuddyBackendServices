package com.petbuddy.social_feed.controller;

import com.petbuddy.social_feed.dto.CreatePostDTO;
import com.petbuddy.social_feed.dto.PostDTO;
import com.petbuddy.social_feed.service.MediaService;
import com.petbuddy.social_feed.service.PostService;
import com.petbuddy.social_feed.dto.DeletePostDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.petbuddy.social_feed.dto.FileMetadataRequest;
import com.petbuddy.social_feed.dto.PresignedUrlResponse;
import java.net.URI;
import java.util.stream.Collectors;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/posts")
@Validated
@Slf4j
@RequiredArgsConstructor
public class PostController {
    
    private final PostService postService;
    private final MediaService mediaService;

    @PostMapping("/presigned-urls")
    public ResponseEntity<List<PresignedUrlResponse>> getUploadUrls(@RequestBody List<FileMetadataRequest> files) {
        List<PresignedUrlResponse> responses = files.stream()
                .map(file -> mediaService.generatePresignedUrl(file.getExtension(), file.getContentType()))
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responses);
    }

    @PostMapping
    public ResponseEntity<PostDTO> createPost(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreatePostDTO createPostDTO,
            UriComponentsBuilder uriBuilder) {
        
        log.info("Creating post for user: {}", userId);
        
        // Set idempotency key from header if not in DTO
        if (StringUtils.hasText(idempotencyKey) && !StringUtils.hasText(createPostDTO.getIdempotencyKey())) {
            createPostDTO.setIdempotencyKey(idempotencyKey);
        }
        
        PostDTO created = postService.createPost(userId, createPostDTO);
        
        URI location = uriBuilder.path("/api/v1/posts/{id}")
                .buildAndExpand(created.getPostId())
                .toUri();
        
        log.info("Post created successfully with ID: {}", created.getPostId());
        return ResponseEntity.created(location).body(created);
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<DeletePostDTO> deletePost(@RequestHeader("X-User-Id") Long userId, @PathVariable Long postId) {
        postService.deletePost(userId, postId);
        return ResponseEntity.ok().build();
    }
}
