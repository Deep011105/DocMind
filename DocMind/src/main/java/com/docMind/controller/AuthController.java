package com.docMind.controller;

import com.docMind.dto.ApiResponse;
import com.docMind.dto.AuthRequest;
import com.docMind.dto.AuthResponse;
import com.docMind.dto.RegisterRequest;
import com.docMind.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {

        AuthResponse response = authService.register(request);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @RequestBody AuthRequest request) {

        AuthResponse response = authService.login(request);

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}