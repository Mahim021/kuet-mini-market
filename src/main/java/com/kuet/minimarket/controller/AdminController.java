package com.kuet.minimarket.controller;

import com.kuet.minimarket.dto.UserResponse;
import com.kuet.minimarket.security.CustomUserDetails;
import com.kuet.minimarket.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    // List all users (excludes the currently logged-in admin)
    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> getAllUsers(Authentication authentication) {
        Long currentAdminId = getCurrentAdminId(authentication);
        return ResponseEntity.ok(adminService.getAllUsers(currentAdminId));
    }

    // Activate a user account
    @PatchMapping("/users/{id}/activate")
    public ResponseEntity<UserResponse> activateUser(@PathVariable Long id, Authentication authentication) {
        Long currentAdminId = getCurrentAdminId(authentication);
        return ResponseEntity.ok(adminService.activateUser(id, currentAdminId));
    }

    // Deactivate a user account
    @PatchMapping("/users/{id}/deactivate")
    public ResponseEntity<UserResponse> deactivateUser(@PathVariable Long id, Authentication authentication) {
        Long currentAdminId = getCurrentAdminId(authentication);
        return ResponseEntity.ok(adminService.deactivateUser(id, currentAdminId));
    }

    private Long getCurrentAdminId(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return userDetails.getUser().getId();
    }
}
