package com.kuet.minimarket.service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

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

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    @Test
    void register_success() {
        RegisterRequest req = new RegisterRequest();
        req.setFullName("Alice");
        req.setEmail("alice@example.com");
        req.setPassword("secret123");
        req.setRoles(List.of("BUYER"));

        Role buyerRole = new Role(1L, RoleName.BUYER);

        when(userRepository.existsByEmail(req.getEmail())).thenReturn(false);
        when(roleRepository.findByName(RoleName.BUYER)).thenReturn(Optional.of(buyerRole));
        when(passwordEncoder.encode(req.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtUtil.generateToken(any())).thenReturn("jwt-token");

        AuthResponse resp = authService.register(req);

        assertThat(resp.getToken()).isEqualTo("jwt-token");
        assertThat(resp.getEmail()).isEqualTo("alice@example.com");
        assertThat(resp.getRoles()).contains("BUYER");
        verify(passwordEncoder).encode("secret123");
    }

    @Test
    void register_duplicateEmail_throws() {
        RegisterRequest req = new RegisterRequest();
        req.setFullName("Dup");
        req.setEmail("dup@example.com");
        req.setPassword("secret123");

        when(userRepository.existsByEmail(req.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessageContaining("dup@example.com");
    }

    @Test
    void register_passwordIsEncoded() {
        RegisterRequest req = new RegisterRequest();
        req.setFullName("Bob");
        req.setEmail("bob@example.com");
        req.setPassword("plaintext");
        req.setRoles(List.of("BUYER"));

        Role buyerRole = new Role(1L, RoleName.BUYER);
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(roleRepository.findByName(any())).thenReturn(Optional.of(buyerRole));
        when(passwordEncoder.encode("plaintext")).thenReturn("$2a$10$encodedHash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            assertThat(u.getPassword()).isEqualTo("$2a$10$encodedHash");
            return u;
        });
        when(jwtUtil.generateToken(any())).thenReturn("tok");

        authService.register(req);
        verify(passwordEncoder).encode("plaintext");
    }

    @Test
    void login_success() {
        LoginRequest req = new LoginRequest();
        req.setEmail("bob@example.com");
        req.setPassword("pass");

        Role role = new Role(1L, RoleName.SELLER);
        User user = User.builder()
                .email("bob@example.com")
                .password("encoded")
                .enabled(true)
                .roles(new HashSet<>(Set.of(role)))
                .build();
        CustomUserDetails userDetails = new CustomUserDetails(user);
        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(jwtUtil.generateToken(userDetails)).thenReturn("seller-token");

        AuthResponse resp = authService.login(req);

        assertThat(resp.getToken()).isEqualTo("seller-token");
        assertThat(resp.getEmail()).isEqualTo("bob@example.com");
        assertThat(resp.getRoles()).contains("SELLER");
    }

    @Test
    void login_invalidCredentials_throws() {
        LoginRequest req = new LoginRequest();
        req.setEmail("nobody@test.com");
        req.setPassword("wrong");

        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void login_disabledUser_throws() {
        LoginRequest req = new LoginRequest();
        req.setEmail("disabled@example.com");
        req.setPassword("pass");

        when(authenticationManager.authenticate(any()))
                .thenThrow(new DisabledException("Account is disabled"));

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(DisabledException.class);
    }

    @Test
    void roleMapping_works() {
        Role adminRole = new Role(1L, RoleName.ADMIN);
        User user = User.builder()
                .email("admin@test.com")
                .password("pass")
                .enabled(true)
                .roles(new HashSet<>(Set.of(adminRole)))
                .build();
        CustomUserDetails userDetails = new CustomUserDetails(user);

        assertThat(userDetails.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_ADMIN");
    }
}
