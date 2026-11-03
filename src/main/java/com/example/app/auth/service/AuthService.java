package com.example.app.auth.service;

import com.example.app.auth.dto.AuthResponse;
import com.example.app.auth.dto.LoginRequest;
import com.example.app.auth.dto.RegisterRequest;
import com.example.app.auth.mapper.AuthMapper;
import com.example.app.auth.security.JwtTokenProvider;
import com.example.app.user.dto.UserResponse;
import com.example.app.user.entity.User;
import com.example.app.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthMapper authMapper;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        UserResponse userResponse = userService.register(
            request.getFirstName(),
            request.getLastName(),
            request.getEmail(),
            request.getPassword()
        );

        String token = jwtTokenProvider.generateTokenFromEmail(userResponse.getEmail());
        return authMapper.toAuthResponse(token, jwtTokenProvider.getExpirationMs(), userResponse);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = (User) authentication.getPrincipal();
        UserResponse userResponse = UserResponse.from(user);
        String token = jwtTokenProvider.generateToken(authentication);

        return authMapper.toAuthResponse(token, jwtTokenProvider.getExpirationMs(), userResponse);
    }

}