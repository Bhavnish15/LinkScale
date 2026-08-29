package com.linkedin.userservice.service;

import com.linkedin.userservice.dto.UserResponse;
import com.linkedin.userservice.entity.Connection;
import com.linkedin.userservice.entity.ConnectionStatus;
import com.linkedin.userservice.entity.User;
import com.linkedin.userservice.repository.ConnectionRepository;
import com.linkedin.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {

    private final ConnectionRepository connectionRepository;
    private final UserRepository userRepository;
    private final S3Client s3Client;

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String CONNECTION_REQUESTED_TOPIC = "connection.requested";
    private static final String CONNECTION_ACCEPTED_TOPIC = "connection.accepted";
    private static final String USER_UPDATED_TOPIC = "user.updated";

    @Value("${aws.s3.bucket}")
    private String bucketName;

    public String sendConnectionRequest(String receiverId, String requesterId){
        if(connectionRepository.existsByRequesterIdAndReceiverId(requesterId, receiverId)){
            throw new RuntimeException("Connection request already sent");
        }

        Connection connection = new Connection();
        connection.setRequesterId(requesterId);
        connection.setReceiverId(receiverId);
        connection.setStatus(ConnectionStatus.PENDING);

        connectionRepository.save(connection);

        //* Publish connection.requested event
        Map<String, Object> connectionRequestEvent = new HashMap<>();
        connectionRequestEvent.put("requestId", requesterId);
        connectionRequestEvent.put("receiverId", receiverId);

        kafkaTemplate.send(CONNECTION_REQUESTED_TOPIC, requesterId, connectionRequestEvent);

        log.info("Connection Request Send: {} --> {}", requesterId, receiverId);
        return "Connection Request Sent";
    }


    public String acceptConnectionRequest(String connectionId) {
        log.info("Accepting Connection Request sent by: {}", connectionId);

        Connection connection = connectionRepository.findById(connectionId).orElseThrow(() -> new RuntimeException("Connection not found: "+ connectionId));
        connection.setStatus(ConnectionStatus.CONNECTED);
        connectionRepository.save(connection);

        //* Publish connection.accepted event
        Map<String, Object> connectionAcceptedEvent = new HashMap<>();
        connectionAcceptedEvent.put("requestId",connection.getRequesterId());
        connectionAcceptedEvent.put("receiverId", connection.getReceiverId());

        kafkaTemplate.send(CONNECTION_ACCEPTED_TOPIC, connectionId, connectionAcceptedEvent);

        log.info("Connection Accepted: {}", connectionId);
        return "Connection Request Accepted";
    }

    public List<UserResponse> getConnections(String userId){
        List<Connection> connections = connectionRepository.findByRequesterIdAndStatus(userId, ConnectionStatus.CONNECTED);

        return connections.stream()
                .map(c -> getUserProfile(c.getReceiverId()))
                .collect(Collectors.toList());
    }

    public UserResponse getUserProfile(String userId){
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found: "+ userId));
        return mapToResponse(user);
    }


    public UserResponse updateProfile(String userId, UserResponse request) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found: "+ userId));

        user.setHeadline(request.getHeadline());
        user.setAbout(request.getAbout());
        user.setLocation(request.getLocation());
        user.setSkills(request.getSkills());

        User savedUser = userRepository.save(user);

        //* Publish Update event
        Map<String, Object> updateUserEvent = new HashMap<>();
        updateUserEvent.put("userId", savedUser.getId());
        updateUserEvent.put("firstname", savedUser.getFirstname());
        updateUserEvent.put("lastname", savedUser.getLastname());
        updateUserEvent.put("headline", savedUser.getHeadline());
        updateUserEvent.put("location", savedUser.getLocation());
        updateUserEvent.put("skills", savedUser.getSkills());

        kafkaTemplate.send(USER_UPDATED_TOPIC, savedUser.getId(), updateUserEvent);

        log.info("user.updated event published: {}", savedUser.getId());

        return mapToResponse(savedUser);
    }


    public UserResponse updateProfilePhoto(String userId, MultipartFile file) {
        log.info("User wants to change their profile photo: {}", userId);

        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found: "+ userId));

        // Delete old photo if present
        if(user.getProfilePhotoUrl() != null){
            String OldKey = extractKeyFromUrl(user.getProfilePhotoUrl());
            deleteObject(OldKey);
        }

        // Build the unique key for the new photo
        String extension = getFileExtension(file.getOriginalFilename());
        String key = "profile-photo/"+ userId + "/" + UUID.randomUUID() + extension;

        // Upload to S3
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(file.getContentType())
                    .build();
            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(
                    file.getInputStream(), file.getSize()));
        } catch (Exception e) {
            log.error("Failed to upload profile photo for user {}", userId, e);
            throw new RuntimeException("Failed to upload profile photo", e);
        }

        String photoUrl = buildPublicUrl(key);

        user.setProfilePhotoUrl(photoUrl);
        User updatedUser = userRepository.save(user);

        log.info("Profile photo updated for user {}: {}", updatedUser.getId(), photoUrl);
        return mapToResponse(updatedUser);
    }

    private void deleteObject(String key){
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build()
            );
        }catch (Exception e){
            log.warn("Failed to delete old profile photo, key={}", key);
        }
    }

    private String extractKeyFromUrl(String url){
        // e.g. https://bucket.s3.region.amazonaws.com/profile-photos/xxx/yyy.jpg
        int idx = url.indexOf(".com/");
        return url.substring(idx + 5);
    }

    private String buildPublicUrl(String key){
        return s3Client.utilities()
                .getUrl(GetUrlRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .build()
                ).toString();
    }

    private String getFileExtension(String filename) {
        if(filename == "" || !filename.contains(".")) return null;
        return filename.substring(filename.lastIndexOf("."));
    }



    private UserResponse mapToResponse(User user){
        UserResponse userResponse = new UserResponse();
        userResponse.setId(user.getId());
        userResponse.setEmail(user.getEmail());
        userResponse.setFirstname(user.getFirstname());
        userResponse.setLastname(user.getLastname());
        userResponse.setHeadline(user.getHeadline());
        userResponse.setAbout(user.getAbout());
        userResponse.setLocation(user.getLocation());
        userResponse.setProfilePhotoUrl(user.getProfilePhotoUrl());
        userResponse.setCoverPhotoUrl(user.getCoverPhotoUrl());
        userResponse.setRole(user.getRole());
        userResponse.setSkills(user.getSkills());
        userResponse.setCreatedAt(user.getCreatedAt());

        return userResponse;
    }
}


















