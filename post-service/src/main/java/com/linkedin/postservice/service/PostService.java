package com.linkedin.postservice.service;

import com.linkedin.postservice.entity.Comment;
import com.linkedin.postservice.entity.Like;
import com.linkedin.postservice.entity.Post;
import com.linkedin.postservice.repository.CommentRepository;
import com.linkedin.postservice.repository.LikeRepository;
import com.linkedin.postservice.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final LikeRepository likeRepository;
    private final CommentRepository commentRepository;

    private final S3Service s3Service;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String POST_CREATION_TOPIC = "post.created";
    private static final String POST_LIKED_TOPIC = "post.liked";
    private static final String POST_COMMENT_TOPIC = "post.comment";


    /**
     * Create Post
     * Optionally upload image to S3
     * Produce post.created event to Kafka
     * Feed Service and Search Service will consume this Kafka event
     *
     * @param authorId
     * @param content
     * @param image
     * @return
     */

    public Post createPost(String authorId, String content, MultipartFile image) {
        log.info("Creating a post of: {} content is : {} image: {}", authorId, content, image);

        Post newPost = new Post();
        newPost.setAuthorId(authorId);
        newPost.setContent(content);

        if (image != null && !image.isEmpty()) {

            String imageUrl = s3Service.uploadFile(authorId, image);
            newPost.setImageUrl(imageUrl);
        }
        Post savedPost = postRepository.save(newPost);

        Map<String, Object> PostCreatedEvent = new HashMap<>();

        PostCreatedEvent.put("id", savedPost.getId());
        PostCreatedEvent.put("authorId", savedPost.getAuthorId());
        PostCreatedEvent.put("content", savedPost.getContent());
        PostCreatedEvent.put("imageUrl", savedPost.getImageUrl());
        PostCreatedEvent.put("createdAt", savedPost.getCreatedAt()).toString();

        kafkaTemplate.send(POST_CREATION_TOPIC, authorId, PostCreatedEvent);   //* Produces Post Creation Kafka Event

        log.info("Post: {} is created successfully", savedPost);

        return savedPost;
    }

    /**
     * Getting Post with postId
     * @param postId
     * @return
     */

    public Post getPost(String postId) {
        log.info("Fetching all the posts");
        Post posts = postRepository.findById(postId).orElseThrow(
                () -> new RuntimeException("Post not found with id: "+ postId));
        return posts;
    }

    public List<Post> getAllPost() {
        List<Post> posts = postRepository.findAll();
        return new ArrayList<>(posts);
    }


    public List<Post> getAuthorPost(String authorId){
        List<Post> userPosts = postRepository.findByAuthorIdOrderByCreatedAtDesc(authorId);
        return userPosts.stream()
                .map(p -> p)
                .collect(Collectors.toList());
    }

    /**
     * Like or Unlike a post
     * @param postId
     * @param userId
     * @return
     */

    public String LikePost(String postId, String userId){
        log.info("Liking a post: {} by user: {}", postId, userId);
        Post post = getPost(postId);

        if(likeRepository.existsByPostIdAndUserId(postId, userId)){

            // Unlike
            likeRepository.findByPostIdAndUserId(postId, userId).ifPresent(likeRepository::delete);
            post.setLikeCount(post.getLikeCount() - 1);
            postRepository.save(post);
            return "Post unliked successfully";
        }

        // Like
        Like like = new Like();
        like.setPostId(postId);
        like.setUserId(userId);
        likeRepository.save(like);
        post.setLikeCount(post.getLikeCount() + 1);
        postRepository.save(post);

        Map<String, Object> postLikeEvent = new HashMap<>();
        postLikeEvent.put("postId", postId);
        postLikeEvent.put("userId", userId);
        postLikeEvent.put("authorId", post.getAuthorId());

        kafkaTemplate.send(POST_LIKED_TOPIC, postId, postLikeEvent);   //* Produces Post Like Kafka Event

        return "Post liked successfully";
    }

    /**
     * User can Add a comment on a post
     * @param postId
     * @param authorId
     * @param content
     * @return
     */

    public Comment addComment(String postId, String authorId, String content) {
        log.info("Commenting on Post: {} by user: {} with content: {}", postId, authorId, content);

        Post post = getPost(postId);

        Comment newComment = new Comment();
        newComment.setPostId(postId);
        newComment.setContent(content);
        newComment.setAuthorId(authorId);

        Comment savedComment = commentRepository.save(newComment);

        post.setCommentCount(post.getCommentCount() + 1);
        postRepository.save(post);

        Map<String, Object> postCommentEvent = new HashMap<>();
        postCommentEvent.put("postId", postId);
        postCommentEvent.put("contentId", savedComment.getId());
        postCommentEvent.put("authorId", post.getAuthorId());
        postCommentEvent.put("postAuthorId", post.getAuthorId());

        kafkaTemplate.send(POST_COMMENT_TOPIC, postId, postCommentEvent);  //* Produces Post Comment Kafka Event
        log.info("Comment is added successfully: {}", newComment.getId());

        return newComment;
    }


    /**
     * Getting comments of a particular post
     * @param postId
     * @return
     */

    public List<Comment> getComment(String postId) {
        log.info("Getting all the comments of a post: {}", postId);
        List<Comment> postComments = commentRepository.findByPostIdOrderByCreatedAtDesc(postId).stream().toList();
        return postComments;
    }

    /**
     *
     * @param postId
     * @param userId
     * @return
     */

    public String deletePost(String postId, String userId){
        log.info("Deleting a post: {} if user: {}", postId, userId);

        Post post = getPost(postId);

        if(!post.getAuthorId().equals(userId)){
            throw new RuntimeException("This post is not written by this Author: "+ userId);
        }

        postRepository.delete(post);
        return "Post is deleted successfully";
    }




}














