package com.alisonpariela.blogmanager.controller;

import com.alisonpariela.blogmanager.DTO.AuthResponse;

import com.alisonpariela.blogmanager.DTO.LoginRequest;
import com.alisonpariela.blogmanager.DTO.RegisterRequest;

import com.alisonpariela.blogmanager.service.AuthService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.alisonpariela.blogmanager.model.User;

import java.util.Map;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

@AllArgsConstructor
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestParam String refreshToken) {
        return ResponseEntity.ok(authService.refreshToken(refreshToken));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal User userDetails) {
        return ResponseEntity.ok(Map.of(
                "id", userDetails.getId(),
                "username", userDetails.getUsername(),
                "authorities", userDetails.getAuthorities()
        ));
    }
}
