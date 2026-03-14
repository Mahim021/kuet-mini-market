package com.kuet.minimarket.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuet.minimarket.entity.Role;
import com.kuet.minimarket.entity.RoleName;
import com.kuet.minimarket.entity.User;
import com.kuet.minimarket.repository.OrderRepository;
import com.kuet.minimarket.repository.ProductRepository;
import com.kuet.minimarket.repository.RoleRepository;
import com.kuet.minimarket.repository.UserRepository;
import com.kuet.minimarket.security.CustomUserDetails;
import com.kuet.minimarket.security.CustomUserDetailsService;
import com.kuet.minimarket.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.HashSet;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class AdminControllerIntegrationTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private CustomUserDetailsService userDetailsService;
    @Autowired private WebApplicationContext webApplicationContext;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
        orderRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();
        roleRepository.save(new Role(null, RoleName.BUYER));
        roleRepository.save(new Role(null, RoleName.SELLER));
        roleRepository.save(new Role(null, RoleName.ADMIN));
    }

    private User createAndSaveUser(String email, String fullName, RoleName roleName) {
        Set<Role> roles = new HashSet<>();
        roleRepository.findByName(roleName).ifPresent(roles::add);
        User user = User.builder()
                .fullName(fullName).email(email)
                .password(passwordEncoder.encode("password"))
                .enabled(true).roles(roles).build();
        return userRepository.save(user);
    }

    private String getToken(String email) {
        CustomUserDetails userDetails = (CustomUserDetails) userDetailsService.loadUserByUsername(email);
        return jwtUtil.generateToken(userDetails);
    }

    // Integration test 7: ADMIN can list users — admin account excluded from list
    @Test
    void getAllUsers_asAdmin_returns200WithUserList() throws Exception {
        createAndSaveUser("admin@admin.com", "Admin User", RoleName.ADMIN);
        createAndSaveUser("buyer@admin.com", "Buyer User", RoleName.BUYER);
        String adminToken = getToken("admin@admin.com");

        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].email").value("buyer@admin.com"));
    }

    // Integration test 8: ADMIN can deactivate a user — returns 200 with enabled=false
    @Test
    void deactivateUser_asAdmin_returns200WithEnabledFalse() throws Exception {
        createAndSaveUser("admin2@admin.com", "Admin2", RoleName.ADMIN);
        User target = createAndSaveUser("target@admin.com", "Target User", RoleName.BUYER);
        String adminToken = getToken("admin2@admin.com");

        mockMvc.perform(patch("/api/admin/users/" + target.getId() + "/deactivate")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false))
                .andExpect(jsonPath("$.email").value("target@admin.com"));
    }

    // Integration test 9: BUYER cannot access admin endpoints — returns 403
    @Test
    void getAllUsers_asBuyer_returns403() throws Exception {
        createAndSaveUser("buyer2@admin.com", "Buyer2", RoleName.BUYER);
        String buyerToken = getToken("buyer2@admin.com");

        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isForbidden());
    }

    // Integration test 10: ADMIN can activate a previously deactivated user
    @Test
    void activateUser_asAdmin_returns200WithEnabledTrue() throws Exception {
        createAndSaveUser("admin3@admin.com", "Admin3", RoleName.ADMIN);
        User disabledUser = User.builder()
                .fullName("Disabled User").email("disabled@admin.com")
                .password(passwordEncoder.encode("password"))
                .enabled(false)
                .roles(new HashSet<>(roleRepository.findByName(RoleName.BUYER).stream().toList()))
                .build();
        disabledUser = userRepository.save(disabledUser);
        String adminToken = getToken("admin3@admin.com");

        mockMvc.perform(patch("/api/admin/users/" + disabledUser.getId() + "/activate")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true));
    }

    // Integration test 11: Unauthenticated request to admin endpoint returns 401
    @Test
    void getAllUsers_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized());
    }

    // Integration test 12: ADMIN cannot deactivate themselves — returns 403
    @Test
    void deactivateUser_selfDeactivate_returns403() throws Exception {
        User adminSelf = createAndSaveUser("admin4@admin.com", "Admin Self", RoleName.ADMIN);
        String adminToken = getToken("admin4@admin.com");

        mockMvc.perform(patch("/api/admin/users/" + adminSelf.getId() + "/deactivate")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden());
    }
}
