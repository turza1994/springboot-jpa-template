package com.example.app.auth.mapper;

import com.example.app.auth.dto.AuthResponse;
import com.example.app.user.dto.UserResponse;
import org.springframework.stereotype.Component;

@Component
public class AuthMapper {

    public AuthResponse toAuthResponse(String accessToken, long expiresIn, UserResponse user) {
        return AuthResponse.builder()
            .accessToken(accessToken)
            .tokenType("Bearer")
            .expiresIn(expiresIn)
            .user(user)
            .build();
    }

}