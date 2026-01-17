package com.petbuddy.social_feed.service;

import com.petbuddy.social_feed.config.RabbitMQConfig;
import com.petbuddy.social_feed.dto.CreatePostDTO;
import com.petbuddy.social_feed.dto.DeletePostDTO;
import com.petbuddy.social_feed.dto.PostDTO;
import com.petbuddy.social_feed.dto.UserDTO;
import com.petbuddy.social_feed.entity.Post;
import com.petbuddy.social_feed.entity.ProcessedContent;
import com.petbuddy.social_feed.enums.MediaType;
import com.petbuddy.social_feed.enums.PostStatus;
import com.petbuddy.social_feed.repository.PostRepository;
import com.petbuddy.social_feed.Client.UserServiceClient;
import com.petbuddy.social_feed.Exception.InvalidMediaTypeException;
import com.petbuddy.social_feed.Exception.PostNotFoundException;
import com.petbuddy.social_feed.Exception.UnauthorizedDeleteException;
import com.petbuddy.social_feed.Exception.UserNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final RabbitTemplate rabbitTemplate;
    private final IdempotencyService idempotencyService;
    private final UserServiceClient userServiceClient;
    private final CacheManager cacheManager;

    public PostDTO createPost(Long authorId, CreatePostDTO createPostDTO) {

        if (StringUtils.hasText(createPostDTO.getIdempotencyKey())) {
            Optional<PostDTO> existing = idempotencyService.getResult(
                createPostDTO.getIdempotencyKey(), PostDTO.class);
            if (existing.isPresent()) {
                log.info("Returning cached result for idempotency key: {}", createPostDTO.getIdempotencyKey());
                return existing.get();
            }
        }

        UserDTO user = userServiceClient.getUserById(authorId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + authorId));

        validateMedia(createPostDTO);
        ProcessedContent processedContent = processContent(createPostDTO);
        
        Post post = buildPostEntity(authorId, createPostDTO, processedContent);
        Post savedPost = postRepository.save(post);

        // processMediaAsync(savedPost, createPostDTO); used for cdn distribution and media optimization, transcoding etc using rabbitmq

        // notifyMentionsAsync(savedPost, processedContent.getMentionedUserIds()); notifications handled in notification microservice using rabbitmq

        // evictUserCache(authorId);
        addPostToCache(savedPost);

        PostDTO postDTO = convertToDTO(savedPost, user);

        if (StringUtils.hasText(createPostDTO.getIdempotencyKey())) {
            idempotencyService.storeResult(createPostDTO.getIdempotencyKey(), postDTO, Duration.ofHours(2));
            return postDTO;
        }

        rabbitTemplate.convertAndSend(RabbitMQConfig.POST_EXCHANGE, RabbitMQConfig.RK_POST_CREATED, postDTO);

        return postDTO;
    }

    private void validateMedia(CreatePostDTO createPostDTO) {
        if (createPostDTO.getMediaUrls() != null && !createPostDTO.getMediaUrls().isEmpty()) {
            MediaType detectedType = detectMediaType(createPostDTO.getMediaUrls());
            if (createPostDTO.getMediaType() != detectedType && createPostDTO.getMediaType() != MediaType.BOTH) {
                throw new InvalidMediaTypeException("Media type doesn't match actual media content");
            }
        }
    }

    private MediaType detectMediaType(List<String> mediaUrls) {
        boolean hasImage = mediaUrls.stream().anyMatch(url -> 
            url.matches(".*\\.(jpg|jpeg|png|gif|webp)$"));
        boolean hasVideo = mediaUrls.stream().anyMatch(url -> 
            url.matches(".*\\.(mp4|mov|avi|webm)$"));
        if (hasImage && hasVideo) return MediaType.BOTH;
        if (hasVideo) return MediaType.VIDEO;
        return MediaType.IMAGE;
    }

    private ProcessedContent processContent(CreatePostDTO createPostDTO) {
        Set<String> extractedHashtags = extractHashtags(createPostDTO.getContentText());
        Set<String> extractedMentions = extractMentions(createPostDTO.getContentText());
        
        Set<String> allHashtags = mergeCollections(extractedHashtags, createPostDTO.getHashtags());
        Set<String> allMentions = mergeCollections(extractedMentions, createPostDTO.getMentions());
        
        Set<Long> mentionedUserIds = resolveMentionedUsers(allMentions);
        
        return ProcessedContent.builder()
                .hashtags(new ArrayList<>(allHashtags))
                .mentions(new ArrayList<>(allMentions))
                .mentionedUserIds(mentionedUserIds)
                .build();
    }

    private Set<String> extractHashtags(String content) {
        if (!StringUtils.hasText(content)) return Set.of();
        
        Pattern pattern = Pattern.compile("#(\\w+)");
        Matcher matcher = pattern.matcher(content);
        
        Set<String> hashtags = new HashSet<>();
        while (matcher.find()) {
            hashtags.add(matcher.group());
        }
        return hashtags;
    }
    
    private Set<String> extractMentions(String content) {
        if (!StringUtils.hasText(content)) return Set.of();
        
        Pattern pattern = Pattern.compile("@(\\w+)");
        Matcher matcher = pattern.matcher(content);
        
        Set<String> mentions = new HashSet<>();
        while (matcher.find()) {
            mentions.add(matcher.group());
        }
        return mentions;
    }
    
    private Set<Long> resolveMentionedUsers(Set<String> mentions) {
        if (mentions.isEmpty()) return Set.of();
        
        return mentions.stream()
                .map(mention -> mention.substring(1)) 
                .map(userServiceClient::findUserByUsername)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(UserDTO::getId)
                .collect(Collectors.toSet());
    }
    
    private Post buildPostEntity(Long userId, CreatePostDTO createPostDTO, ProcessedContent processedContent) {
        return Post.builder()
                .userId(userId)
                .contentText(createPostDTO.getContentText())
                .mediaUrls(createPostDTO.getMediaUrls())
                .mediaType(createPostDTO.getMediaType())
                .hashtags(processedContent.getHashtags())
                .mentions(processedContent.getMentions())
                .mentionedUserIds(new ArrayList<>(processedContent.getMentionedUserIds()))
                .latitude(createPostDTO.getLatitude())
                .longitude(createPostDTO.getLongitude())
                .locationName(createPostDTO.getLocationName())
                .mediaVisibility(createPostDTO.getMediaVisibility())
                .status(PostStatus.ACTIVE)
                .urgency(createPostDTO.getUrgency())
                .channelId(createPostDTO.getChannelId())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .viewCount(0L)
                .likeCount(0L)
                .commentCount(0L)
                .build();
    }
    
    // @Async("taskExecutor")
    // public void processMediaAsync(Post post, CreatePostDTO createPostDTO) {
    //     try {
    //         if (post.getMediaUrls() != null && !post.getMediaUrls().isEmpty()) {
    //             mediaServiceClient.processPostMedia(
    //                 post.getPostId(),
    //                 post.getMediaUrls(), 
    //                 createPostDTO.getMediaType()
    //             );
    //             log.info("Media processing completed for post: {}", post.getPostId());
    //         }
    //     } catch (Exception e) {
    //         log.error("Media processing failed for post: {}", post.getPostId(), e);
    //     }
    // }
    
    // @Async("taskExecutor")
    // public void notifyMentionsAsync(Post post, Set<Long> mentionedUserIds) {
    //     try {
    //         if (!mentionedUserIds.isEmpty()) {
    //             notificationServiceClient.sendMentionNotifications(
    //                 post.getPostId(),
    //                 post.getUserId(),
    //                 new ArrayList<>(mentionedUserIds)
    //             );
    //             log.info("Mention notifications sent for post: {}", post.getPostId());
    //         }
    //     } catch (Exception e) {
    //         log.error("Failed to send mention notifications for post: {}", post.getPostId(), e);
    //     }
    // }
    
    private void evictUserCache(Post post) {
        try {
            Cache cache = cacheManager.getCache("user-posts");
            if (cache != null) {
                String compositeKey = post.getUserId() + ":" + post.getPostId();
                cache.evict(compositeKey);
            }
        } catch (Exception e) {
            log.warn("Failed to evict cache for user: {}", post.getUserId(), e);
        }
    }

    private void addPostToCache(Post post) {
        try {
            Cache cache = cacheManager.getCache("user-posts");
            if (cache != null) {
                String compositeKey = post.getUserId() + ":" + post.getPostId();
                cache.put(compositeKey, post);
            }
        } catch (Exception e) {
            log.warn("Failed to add post to cache for user: {}", post.getUserId(), e);
        }
    }
    
    private PostDTO convertToDTO(Post post, UserDTO user) {
        return PostDTO.builder()
                .postId(post.getPostId())
                .contentText(post.getContentText())
                .mediaUrls(post.getMediaUrls())
                .mediaType(post.getMediaType())
                .hashtags(post.getHashtags())
                .mentions(post.getMentions())
                .latitude(post.getLatitude())
                .longitude(post.getLongitude())
                .locationName(post.getLocationName())
                .mediaVisibility(post.getMediaVisibility())
                .userId(post.getUserId())
                .username(user.getUsername())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .viewCount(post.getViewCount())
                .likeCount(post.getLikeCount())
                .commentCount(post.getCommentCount())
                .build();
    }
    
    private <T> Set<T> mergeCollections(Set<T> set1, List<T> list2) {
        Set<T> result = new HashSet<>(set1);
        if (list2 != null) {
            result.addAll(list2);
        }
        return result;
    }

    public DeletePostDTO deletePost(Long userId, Long postId) {

        Post post = postRepository.findByPostIdAndStatus(postId, PostStatus.ACTIVE)
                .orElseThrow(() -> new PostNotFoundException("Post not found or already deleted"));

        if (!post.getUserId().equals(userId)) {
            throw new UnauthorizedDeleteException("User is not authorized to delete this post");
        }

        post.setStatus(PostStatus.DELETED);
        postRepository.save(post);

        rabbitTemplate.convertAndSend(RabbitMQConfig.POST_EXCHANGE, RabbitMQConfig.RK_POST_DELETED, postId);
        evictUserCache(post);


        return DeletePostDTO.builder()
                .postSuccessfulDeleted(true)
                .build();
    }
}
