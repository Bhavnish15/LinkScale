package com.linkedin.userservice.controller;

import com.linkedin.userservice.dto.AuthResponse;
import com.linkedin.userservice.dto.LoginRequest;
import com.linkedin.userservice.dto.RegisterRequest;
import com.linkedin.userservice.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;


    //** Register new User
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest registerRequest){
        log.info("Registering new user: {}", registerRequest.getEmail());
        // Implementation of new User Registration
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(registerRequest));
    }

    //** Login User
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest loginRequest){
        log.info("Logging in user: {}", loginRequest.getEmail());
        // Implementation of new User Login
        return ResponseEntity.status(HttpStatus.OK).body(authService.login(loginRequest));
    }
}
