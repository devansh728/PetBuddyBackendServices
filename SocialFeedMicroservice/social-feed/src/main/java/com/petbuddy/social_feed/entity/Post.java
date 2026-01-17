package com.petbuddy.social_feed.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.petbuddy.social_feed.enums.Urgency;
import java.time.Instant;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.petbuddy.social_feed.enums.MediaType;
import com.petbuddy.social_feed.enums.MediaVisibility;
import com.petbuddy.social_feed.enums.PostStatus;


@Entity
@Table(name = "posts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long postId;
    
    @Column(nullable = false)
    private Long userId;

    private Long channelId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Urgency urgency;
    
    @Column(length = 2200)
    private String contentText;
    
    @ElementCollection
    @CollectionTable(name = "post_media_urls", joinColumns = @JoinColumn(name = "post_id"))
    private List<String> mediaUrls;

    @ElementCollection
    @CollectionTable(name = "post_thumbnail_urls", joinColumns = @JoinColumn(name = "post_id"))
    private List<String> thumbnailUrls; 
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MediaType mediaType;
    
    @ElementCollection
    @CollectionTable(name = "post_hashtags", joinColumns = @JoinColumn(name = "post_id"))
    private List<String> hashtags;
    
    @ElementCollection
    @CollectionTable(name = "post_mentions", joinColumns = @JoinColumn(name = "post_id"))
    private List<String> mentions;
    
    @ElementCollection
    @CollectionTable(name = "post_mentioned_user_ids", joinColumns = @JoinColumn(name = "post_id"))
    private List<Long> mentionedUserIds;
    
    private Double latitude;
    private Double longitude;
    
    @Column(length = 100)
    private String locationName;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MediaVisibility mediaVisibility;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PostStatus status;
    
    @CreationTimestamp
    private Instant createdAt;
    
    @UpdateTimestamp
    private Instant updatedAt;
    
    private Long viewCount;
    private Long likeCount;
    private Long commentCount;
}