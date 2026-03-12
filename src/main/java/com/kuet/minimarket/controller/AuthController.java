package com.kuet.minimarket.controller;

import com.kuet.minimarket.dto.AuthResponse;
import com.kuet.minimarket.dto.LoginRequest;
import com.kuet.minimarket.dto.RegisterRequest;
import com.kuet.minimarket.service.AuthService;
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
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        // JWT is stateless; client discards the token
        return ResponseEntity.noContent().build();
    }
}
