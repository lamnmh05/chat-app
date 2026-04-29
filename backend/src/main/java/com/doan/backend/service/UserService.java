package com.doan.backend.service;

import com.doan.backend.domain.document.UserDocument;
import com.doan.backend.domain.enums.UserStatus;
import com.doan.backend.dto.user.UpdateCurrentUserRequest;
import com.doan.backend.dto.user.UserResponse;
import com.doan.backend.exception.NotFoundException;
import com.doan.backend.repository.UserRepository;
import com.doan.backend.support.EntityMapper;
import java.time.Instant;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserDocument getRequiredUser(String userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("User not found"));
    }

    public UserResponse getCurrentUser(String userId) {
        return EntityMapper.toUserResponse(getRequiredUser(userId));
    }

    public UserResponse updateCurrentUser(String userId, UpdateCurrentUserRequest request) {
        UserDocument user = getRequiredUser(userId);
        if (request.displayName() != null && !request.displayName().isBlank()) {
            user.setDisplayName(request.displayName().trim());
        }
        if (request.avatarUrl() != null) {
            user.setAvatarUrl(request.avatarUrl().trim().isEmpty() ? null : request.avatarUrl().trim());
        }
        if (request.status() != null) {
            user.setStatus(request.status());
        }
        user.setUpdatedAt(Instant.now());
        return EntityMapper.toUserResponse(userRepository.save(user));
    }

    public UserResponse getUserById(String userId) {
        return EntityMapper.toUserResponse(getRequiredUser(userId));
    }

    public void updateStatus(String userId, UserStatus status) {
        UserDocument user = getRequiredUser(userId);
        user.setStatus(status);
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);
    }
}
