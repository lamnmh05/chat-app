package com.doan.backend.controller;

import com.doan.backend.dto.user.UpdateCurrentUserRequest;
import com.doan.backend.dto.user.UserResponse;
import com.doan.backend.security.AuthenticatedUser;
import com.doan.backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public UserResponse getCurrentUser(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return userService.getCurrentUser(currentUser.userId());
    }

    @PutMapping("/me")
    public UserResponse updateCurrentUser(
        @AuthenticationPrincipal AuthenticatedUser currentUser,
        @Valid @RequestBody UpdateCurrentUserRequest request
    ) {
        return userService.updateCurrentUser(currentUser.userId(), request);
    }

    @GetMapping("/{userId}")
    public UserResponse getUserById(@PathVariable String userId) {
        return userService.getUserById(userId);
    }
}
