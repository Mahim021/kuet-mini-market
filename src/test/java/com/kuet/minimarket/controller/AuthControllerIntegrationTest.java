package com.kuet.minimarket.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuet.minimarket.dto.LoginRequest;
import com.kuet.minimarket.dto.RegisterRequest;
import com.kuet.minimarket.entity.Role;
import com.kuet.minimarket.entity.RoleName;
import com.kuet.minimarket.entity.User;
import com.kuet.minimarket.repository.RoleRepository;
import com.kuet.minimarket.repository.UserRepository;
import com.kuet.minimarket.security.CustomUserDetails;
import com.kuet.minimarket.security.CustomUserDetailsService;
import com.kuet.minimarket.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;

import java.util.HashSet;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class AuthControllerIntegrationTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private CustomUserDetailsService userDetailsService;
    @Autowired private WebApplicationContext webApplicationContext;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
        userRepository.deleteAll();
        roleRepository.deleteAll();
        roleRepository.save(new Role(null, RoleName.BUYER));
        roleRepository.save(new Role(null, RoleName.ADMIN));
        roleRepository.save(new Role(null, RoleName.SELLER));
    }

    private String createUserAndGetToken(String email, String fullName, RoleName roleName) {
        Set<Role> roles = new HashSet<>();
        roleRepository.findByName(roleName).ifPresent(roles::add);

        User user = User.builder()
                .fullName(fullName)
                .email(email)
                .password(passwordEncoder.encode("password"))
                .enabled(true)
                .roles(roles)
                .build();
        userRepository.save(user);

        CustomUserDetails userDetails = (CustomUserDetails) userDetailsService.loadUserByUsername(email);
        return jwtUtil.generateToken(userDetails);
    }

    @Test
    void register_endpoint_works() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setFullName("Test User");
        req.setEmail("newuser@test.com");
        req.setPassword("password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.email").value("newuser@test.com"))
                .andExpect(jsonPath("$.roles[0]").value("BUYER"));
    }

    @Test
    void register_duplicateEmail_returns409() throws Exception {
        createUserAndGetToken("dup@test.com", "Dup User", RoleName.BUYER);

        RegisterRequest req = new RegisterRequest();
        req.setFullName("Dup Again");
        req.setEmail("dup@test.com");
        req.setPassword("password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test
    void login_validCredentials_returnsToken() throws Exception {
        createUserAndGetToken("loginuser@test.com", "Login User", RoleName.BUYER);

        LoginRequest req = new LoginRequest();
        req.setEmail("loginuser@test.com");
        req.setPassword("password");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.email").value("loginuser@test.com"));
    }

    @Test
    void login_invalidCredentials_returns401() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail("ghost@test.com");
        req.setPassword("wrongpass");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedRoute_blocksUnauthenticated() throws Exception {
        mockMvc.perform(get("/admin/test"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminRoute_accessibleByAdmin() throws Exception {
        String token = createUserAndGetToken("admin@test.com", "Admin User", RoleName.ADMIN);

        mockMvc.perform(get("/admin/test")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void adminRoute_blockedForBuyer() throws Exception {
        String token = createUserAndGetToken("buyer@test.com", "Buyer User", RoleName.BUYER);

        mockMvc.perform(get("/admin/test")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminRoute_blockedForSeller() throws Exception {
        String token = createUserAndGetToken("seller@test.com", "Seller User", RoleName.SELLER);

        mockMvc.perform(get("/admin/test")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void disabledUser_blockedFromLogin() throws Exception {
        Set<Role> roles = new HashSet<>();
        roleRepository.findByName(RoleName.BUYER).ifPresent(roles::add);

        User disabled = User.builder()
                .fullName("Disabled User")
                .email("disabled@test.com")
                .password(passwordEncoder.encode("password"))
                .enabled(false)
                .roles(roles)
                .build();
        userRepository.save(disabled);

        LoginRequest req = new LoginRequest();
        req.setEmail("disabled@test.com");
        req.setPassword("password");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }
}
