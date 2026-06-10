package com.docmind.service;

import com.docmind.dto.request.AuthRequest;
import com.docmind.dto.request.RegisterRequest;
import com.docmind.dto.response.AuthResponse;
import com.docmind.entity.User;
import com.docmind.exception.UserEmailNotExistException;
import com.docmind.repository.UserRepository;
import com.docmind.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthResponse register(RegisterRequest request) {

        log.info("Registration request received for email: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration failed. Email already exists: {}", request.getEmail());
            throw new UserEmailNotExistException("User email already exists");
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role("USER")
                .build();

        userRepository.save(user);

        log.info("User registered successfully: {}", user.getEmail());

        String token = jwtService.generateToken(user.getEmail());

        log.debug("JWT token generated for user: {}", user.getEmail());

        return AuthResponse.builder()
                .token(token)
                .username(user.getUsername())
                .email(user.getEmail())
                .build();
    }

    public AuthResponse login(AuthRequest request) {

        log.info("Login attempt for email: {}", request.getEmail());

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.warn("Login failed. User not found: {}", request.getEmail());
                    return new UserEmailNotExistException("User email not found");
                });

        String token = jwtService.generateToken(request.getEmail());

        log.info("User logged in successfully: {}", user.getEmail());

        return AuthResponse.builder()
                .token(token)
                .username(user.getUsername())
                .email(user.getEmail())
                .build();
    }
}