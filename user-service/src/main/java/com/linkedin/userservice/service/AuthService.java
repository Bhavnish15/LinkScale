package com.linkedin.userservice.service;

import com.linkedin.userservice.dto.AuthResponse;
import com.linkedin.userservice.dto.LoginRequest;
import com.linkedin.userservice.dto.RegisterRequest;
import com.linkedin.userservice.entity.User;
import com.linkedin.userservice.entity.UserRole;
import com.linkedin.userservice.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String USER_CREATED_TOPIC = "user.created";

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    @Value("${jwt.refreshToken-expiration}")
    private long jwtRefreshTokenExpiration;


    public AuthResponse register(RegisterRequest registerRequest){
        log.info("Registering user: {}", registerRequest.getEmail());
        if(userRepository.existsByEmail(registerRequest.getEmail())){
            throw new RuntimeException("User with email " + registerRequest.getEmail() + " already exists!");
        }
        // Proceed with user registration
            User user = User.builder()
                    .firstname(registerRequest.getFirstname())
                    .lastname(registerRequest.getLastname())
                    .email(registerRequest.getEmail())
                    .headline(registerRequest.getHeadline())
                    .location(registerRequest.getLocation())
                    .password(passwordEncoder.encode(registerRequest.getPassword()))
                    .role(UserRole.NORMAL_USER)
                    .build();
        User savedUser = userRepository.save(user);

        log.info("User registered successfully Email: {}, Id: {}", savedUser.getEmail(), savedUser.getId());


        //* Publish user.created event
        //* Search service consume this and indexes user
        Map<String, Object> userCreatedEvent = new HashMap<>();

        userCreatedEvent.put("userId", savedUser.getId());
        userCreatedEvent.put("firstName", savedUser.getFirstname());
        userCreatedEvent.put("lastName", savedUser.getLastname());
        userCreatedEvent.put("email", savedUser.getEmail());
        userCreatedEvent.put("headline", savedUser.getHeadline());
        userCreatedEvent.put("location", savedUser.getLocation());

        kafkaTemplate.send(USER_CREATED_TOPIC, savedUser.getId(), userCreatedEvent);   //* KAFKA EVENT PRODUCES

        log.info("user.created event published: {}", savedUser.getId());

        String token = generateToken(savedUser.getId(), savedUser.getEmail());

        return buildAuthResponse(savedUser, token);
    }

    public AuthResponse login(LoginRequest loginRequest){
        log.info("Login attempt: {}", loginRequest.getEmail());

        User user = userRepository.findByEmail(loginRequest.getEmail()).orElseThrow(() -> new RuntimeException("User Not Found:"+ loginRequest.getEmail()));

        //* BCrypt Varify - Compare raw password with stored hash
        if(!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())){
            throw new RuntimeException("Invalid Credentials");
        }
        log.info("Login Successful: {}", user.getId());

        //* Generate JWT token
        String token = generateToken(user.getId(), user.getEmail());

        return buildAuthResponse(user, token);
    }

    /**
     * Generate JWT Auth Token
     * Used to get a new token when user log in or register for the first time
     * @param userId
     * @param userEmail
     * @return
     */

    private String generateToken(String userId, String userEmail){
        return Jwts.builder()
                .claim("userId", userId)
                .setSubject(userEmail)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Generate refresh token
     * Used to get a new access token when it expires
     * Server validates and return new access token.
     * Client sends refresh token to /auth/refresh endpoint.
     * @param userId
     * @return
     */

    private String generateRefreshToken(String userId){
        return Jwts.builder()
                .claim("userId", userId)
                .setSubject(userId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtRefreshTokenExpiration))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }



    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public AuthResponse buildAuthResponse(User user, String token){
        AuthResponse response = new AuthResponse();

        response.setAccessToken(token);
        response.setRefreshToken(generateRefreshToken(user.getId()));
        response.setEmail(user.getEmail());
        response.setUserId(user.getId());
        response.setFirstname(user.getFirstname());
        response.setLastname(user.getLastname());

        return response;
    }

}

