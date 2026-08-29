package com.linkedin.userservice.dto;

import com.linkedin.userservice.entity.UserRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserResponse {

    private String id;
    private String email;
    private String password;
    private String firstname;
    private String lastname;
    private String headline;
    private String about;
    private String location;
    private String profilePhotoUrl;
    private String coverPhotoUrl;
    private UserRole role;
    private List<String> skills = new ArrayList<>();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
