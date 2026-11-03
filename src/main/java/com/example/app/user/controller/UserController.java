package com.example.app.user.controller;

import com.example.app.user.dto.UserResponse;
import com.example.app.user.service.UserService;
import com.example.app.common.response.ApiResponse;
import com.example.app.common.response.PageResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(HttpServletRequest httpRequest) {
        // The authenticated user's email is available from the security context
        // For now, we'll use a simple approach - in a real app, you'd get this from the auth token
        // This endpoint will be protected and the user can be extracted from the JWT
        String email = httpRequest.getUserPrincipal().getName();
        UserResponse user = userService.getByEmail(email);
        return ResponseEntity.ok(ApiResponse.success(user, (String) httpRequest.getAttribute("requestId")));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {

        UserResponse user = userService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(user, (String) httpRequest.getAttribute("requestId")));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> getAllUsers(
            @PageableDefault(size = 20) Pageable pageable,
            HttpServletRequest httpRequest) {

        Page<UserResponse> users = userService.findAll(pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(users), (String) httpRequest.getAttribute("requestId")));
    }

}