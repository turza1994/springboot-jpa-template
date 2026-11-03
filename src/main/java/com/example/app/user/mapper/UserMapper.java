package com.example.app.user.mapper;

import com.example.app.user.dto.UserResponse;
import com.example.app.user.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        if (user == null) {
            return null;
        }
        return UserResponse.from(user);
    }

}