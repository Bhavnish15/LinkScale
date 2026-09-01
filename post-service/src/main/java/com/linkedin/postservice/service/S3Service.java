package com.linkedin.postservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3Service {

    @Value("${aws.s3.bucket}")
    private String bucketName;

    private final S3Client s3Client;

    public String uploadFile(String authorId, MultipartFile image) {
        String extension = getFileExtension(image.getOriginalFilename());
        String key = "post-image/"+ authorId + "/" + UUID.randomUUID() + extension;
        String url;

        // Upload to S3
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(image.getContentType())
                    .build();
            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(
                    image.getInputStream(), image.getSize()));

            url = buildPublicUrl(key);

            log.info("File uploaded to s3 successfully: {}", url);
        }catch (Exception e){
            log.error("Failed to upload post photo for author {}", authorId, e);
            throw new RuntimeException("Failed to upload post photo", e);
        }

        return url;
    }

    private String buildPublicUrl(String key) {
        return s3Client.utilities()
                .getUrl(GetUrlRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .build()
                ).toString();
    }

    private String getFileExtension(String filename) {
        if(Objects.equals(filename, "") || !filename.contains(("."))) return null;
        return filename.substring(filename.lastIndexOf("."));

    }

}
