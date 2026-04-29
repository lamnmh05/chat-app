package com.doan.backend.service;

import com.doan.backend.domain.document.UserDocument;
import com.doan.backend.domain.enums.UserStatus;
import com.doan.backend.dto.auth.AuthResponse;
import com.doan.backend.dto.auth.LoginRequest;
import com.doan.backend.dto.auth.RegisterRequest;
import com.doan.backend.exception.BadRequestException;
import com.doan.backend.exception.UnauthorizedException;
import com.doan.backend.repository.UserRepository;
import com.doan.backend.security.AuthenticatedUser;
import com.doan.backend.security.JwtTokenProvider;
import com.doan.backend.support.EntityMapper;
import java.time.Instant;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(
        UserRepository userRepository,
        PasswordEncoder passwordEncoder,
        JwtTokenProvider jwtTokenProvider
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsernameIgnoreCase(request.username().trim())) {
            throw new BadRequestException("Username already exists");
        }
        if (userRepository.existsByEmailIgnoreCase(request.email().trim())) {
            throw new BadRequestException("Email already exists");
        }

        Instant now = Instant.now();
        UserDocument user = UserDocument.builder()
            .username(request.username().trim())
            .email(request.email().trim().toLowerCase(Locale.ROOT))
            .displayName(request.displayName().trim())
            .passwordHash(passwordEncoder.encode(request.password()))
            .status(UserStatus.OFFLINE)
            .createdAt(now)
            .updatedAt(now)
            .build();

        UserDocument savedUser = userRepository.save(user);
        return buildAuthResponse(savedUser);
    }

    public AuthResponse login(LoginRequest request) {
        String identifier = request.identifier().trim();
        UserDocument user = identifier.contains("@")
            ? userRepository.findByEmailIgnoreCase(identifier).orElseThrow(() -> new UnauthorizedException("Invalid credentials"))
            : userRepository.findByUsernameIgnoreCase(identifier).orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid credentials");
        }

        user.setStatus(UserStatus.ONLINE);
        user.setUpdatedAt(Instant.now());
        UserDocument savedUser = userRepository.save(user);
        return buildAuthResponse(savedUser);
    }

    private AuthResponse buildAuthResponse(UserDocument user) {
        AuthenticatedUser authenticatedUser = new AuthenticatedUser(user.getId(), user.getUsername(), user.getEmail(), user.getPasswordHash());
        return new AuthResponse(
            jwtTokenProvider.generateAccessToken(authenticatedUser),
            "Bearer",
            EntityMapper.toUserResponse(user)
        );
    }
}
