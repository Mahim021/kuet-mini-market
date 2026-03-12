package com.kuet.minimarket.service;

import com.kuet.minimarket.dto.AuthResponse;
import com.kuet.minimarket.dto.LoginRequest;
import com.kuet.minimarket.dto.RegisterRequest;
import com.kuet.minimarket.entity.Role;
import com.kuet.minimarket.entity.RoleName;
import com.kuet.minimarket.entity.User;
import com.kuet.minimarket.exception.EmailAlreadyExistsException;
import com.kuet.minimarket.repository.RoleRepository;
import com.kuet.minimarket.repository.UserRepository;
import com.kuet.minimarket.security.CustomUserDetails;
import com.kuet.minimarket.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Email already registered: " + request.getEmail());
        }

        List<String> requestedRoles = (request.getRoles() != null && !request.getRoles().isEmpty())
                ? request.getRoles()
                : List.of("BUYER");

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .enabled(true)
                .build();

        for (String roleStr : requestedRoles) {
            RoleName rn;
            try {
                rn = RoleName.valueOf(roleStr.toUpperCase());
            } catch (IllegalArgumentException ignored) {
                continue; // skip unknown role strings
            }
            final RoleName finalRn = rn;
            Role role = roleRepository.findByName(finalRn)
                    .orElseGet(() -> roleRepository.save(new Role(null, finalRn)));
            user.getRoles().add(role);
        }

        // Fallback: if no valid role was provided, assign BUYER
        if (user.getRoles().isEmpty()) {
            Role buyerRole = roleRepository.findByName(RoleName.BUYER)
                    .orElseGet(() -> roleRepository.save(new Role(null, RoleName.BUYER)));
            user.getRoles().add(buyerRole);
        }

        userRepository.save(user);

        CustomUserDetails userDetails = new CustomUserDetails(user);
        String token = jwtUtil.generateToken(userDetails);

        List<String> roles = user.getRoles().stream()
                .map(r -> r.getName().name())
                .collect(Collectors.toList());

        return AuthResponse.builder()
                .id(user.getId())
                .token(token)
                .email(user.getEmail())
                .roles(roles)
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String token = jwtUtil.generateToken(userDetails);

        List<String> roles = userDetails.getUser().getRoles().stream()
                .map(r -> r.getName().name())
                .collect(Collectors.toList());

        return AuthResponse.builder()
                .id(userDetails.getUser().getId())
                .token(token)
                .email(userDetails.getUsername())
                .roles(roles)
                .build();
    }
}
