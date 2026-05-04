package com.doan.backend.service;

import com.doan.backend.domain.document.UserDocument;
import com.doan.backend.domain.enums.UserStatus;
import com.doan.backend.dto.auth.AuthResponse;
import com.doan.backend.dto.auth.LoginRequest;
import com.doan.backend.exception.UnauthorizedException;
import com.doan.backend.repository.UserRepository;
import com.doan.backend.security.AuthenticatedUser;
import com.doan.backend.security.JwtTokenProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthService authService;

    @Test
    void login_ShouldReturnAuthResponse_WhenCredentialsAreValid() {
        // Arrange
        LoginRequest request = new LoginRequest("mavis", "correct_password");
        UserDocument mockUser = UserDocument.builder()
                .id("u1")
                .username("mavis")
                .email("mavis@uit.edu.vn")
                .passwordHash("hashed_password")
                .status(UserStatus.OFFLINE)
                .build();

        Mockito.when(userRepository.findByUsernameIgnoreCase("mavis")).thenReturn(Optional.of(mockUser));
        Mockito.when(passwordEncoder.matches("correct_password", "hashed_password")).thenReturn(true);
        Mockito.when(userRepository.save(Mockito.any(UserDocument.class))).thenReturn(mockUser);
        Mockito.when(jwtTokenProvider.generateAccessToken(Mockito.any(AuthenticatedUser.class))).thenReturn("mock_jwt_token");

        // Act
        AuthResponse response = authService.login(request);

        // Assert
        Assertions.assertNotNull(response);
        Assertions.assertEquals("mock_jwt_token", response.accessToken());
        Assertions.assertEquals("Bearer", response.tokenType());
        Assertions.assertEquals("mavis", response.user().username());
        Assertions.assertEquals(UserStatus.ONLINE, mockUser.getStatus()); // Đảm bảo status được update thành ONLINE
    }

    @Test
    void login_ShouldThrowUnauthorized_WhenPasswordIsWrong() {
        // Arrange
        LoginRequest request = new LoginRequest("mavis", "wrong_password");
        UserDocument mockUser = UserDocument.builder().passwordHash("hashed_password").build();

        Mockito.when(userRepository.findByUsernameIgnoreCase("mavis")).thenReturn(Optional.of(mockUser));
        Mockito.when(passwordEncoder.matches("wrong_password", "hashed_password")).thenReturn(false);

        // Act & Assert
        Assertions.assertThrows(UnauthorizedException.class, () -> authService.login(request));
        Mockito.verify(userRepository, Mockito.never()).save(Mockito.any()); // Đảm bảo không lưu bậy bạ
    }
}