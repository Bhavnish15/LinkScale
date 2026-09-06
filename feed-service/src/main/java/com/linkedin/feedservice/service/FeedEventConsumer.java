package com.linkedin.feedservice.service;

import com.linkedin.feedservice.client.UserServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class FeedEventConsumer {

    private final UserServiceClient userServiceClient;

    private static final String FEED_KEY_PREFIX = "feed";

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${feed.max-size}")
    private int maxFeedSize;

    /**
     * Consume post.created event
     * When a user creates a post - immediately push that post
     * to All their connections feed
     *
     * This is how linkedin/twitter generated feed
     * @param payload
     */

    @KafkaListener(topics = "post.created")
    public void consumePostCreated(@Payload Map<String, Object> payload){
        try{
            String postId = (String) payload.get("postId");
            String authorId = (String) payload.get("authorId");
            Long timeStamp = (Long) payload.get("createdAt");

            //* Get all connections of a author
            List<Map<String, Object>> connections = userServiceClient.getConnections(authorId);

            //* Push post to each connection's feed.
            for(Map<String, Object> connection : connections) {
                String connectionId = (String) connection.get("id");
                String feedKey = FEED_KEY_PREFIX + connectionId;

                //* Add post to feed (as sorted set - score = timestamp)
                redisTemplate.opsForZSet().add(feedKey, postId, timeStamp);
                Long size = redisTemplate.opsForZSet().zCard(feedKey);
                if(size != null && size > maxFeedSize){
                    redisTemplate.opsForZSet().removeRange(feedKey, 0, size - maxFeedSize - 1 );
                }

//                redisTemplate.opsForList().leftPush(feedKey, postId);
//                redisTemplate.opsForList().trim(feedKey, 0, maxFeedSize - 1);
            }

                //* Add post to author's own feed
            String authorFeedKey = FEED_KEY_PREFIX + authorId;
            redisTemplate.opsForZSet().add(authorFeedKey, postId, timeStamp);
            Long authorSize = redisTemplate.opsForZSet().zCard(authorFeedKey);
            if(authorSize != null && authorSize > maxFeedSize) {
                redisTemplate.opsForZSet().removeRange(authorFeedKey, 0, authorSize - maxFeedSize - 1);
            }

//            redisTemplate.opsForList().leftPush(authorFeedKey, postId);
//            redisTemplate.opsForList().trim(authorFeedKey, 0, maxFeedSize - 1);


        }catch(Exception e) {
            log.error("Error in pushing: {}", e.getMessage());
        }
    }
}









