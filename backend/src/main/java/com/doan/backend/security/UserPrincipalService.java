package com.doan.backend.security;

import com.doan.backend.domain.document.UserDocument;
import com.doan.backend.exception.NotFoundException;
import com.doan.backend.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserPrincipalService {
    private final UserRepository userRepository;

    public UserPrincipalService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public AuthenticatedUser loadByUserId(String userId) {
        UserDocument user = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("User not found"));
        return new AuthenticatedUser(user.getId(), user.getUsername(), user.getEmail(), user.getPasswordHash());
    }
}
