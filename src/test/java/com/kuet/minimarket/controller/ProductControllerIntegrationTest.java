package com.kuet.minimarket.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuet.minimarket.dto.ProductRequest;
import com.kuet.minimarket.entity.Role;
import com.kuet.minimarket.entity.RoleName;
import com.kuet.minimarket.entity.User;
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
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class ProductControllerIntegrationTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private CustomUserDetailsService userDetailsService;
    @Autowired private WebApplicationContext webApplicationContext;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
        productRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();
        roleRepository.save(new Role(null, RoleName.BUYER));
        roleRepository.save(new Role(null, RoleName.SELLER));
        roleRepository.save(new Role(null, RoleName.ADMIN));
    }

    private String createUserAndGetToken(String email, String fullName, RoleName roleName) {
        Set<Role> roles = new HashSet<>();
        roleRepository.findByName(roleName).ifPresent(roles::add);
        User user = User.builder()
                .fullName(fullName).email(email)
                .password(passwordEncoder.encode("password"))
                .enabled(true).roles(roles).build();
        userRepository.save(user);
        CustomUserDetails userDetails = (CustomUserDetails) userDetailsService.loadUserByUsername(email);
        return jwtUtil.generateToken(userDetails);
    }

    // Integration test 1: GET /api/products is publicly accessible without authentication
    @Test
    void getProducts_withoutAuth_returns200() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk());
    }

    // Integration test 2: SELLER can create a product — returns 201
    @Test
    void createProduct_asSeller_returns201WithBody() throws Exception {
        String sellerToken = createUserAndGetToken("seller@test.com", "Test Seller", RoleName.SELLER);

        ProductRequest req = new ProductRequest();
        req.setTitle("Wireless Mouse");
        req.setPrice(BigDecimal.valueOf(25.00));
        req.setStock(10);
        req.setDescription("Ergonomic mouse");

        mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + sellerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Wireless Mouse"))
                .andExpect(jsonPath("$.sellerId").exists())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    // Integration test 3: BUYER cannot create a product — returns 403
    @Test
    void createProduct_asBuyer_returns403() throws Exception {
        String buyerToken = createUserAndGetToken("buyer@test.com", "Test Buyer", RoleName.BUYER);

        ProductRequest req = new ProductRequest();
        req.setTitle("Fake Product");
        req.setPrice(BigDecimal.valueOf(10.00));
        req.setStock(1);

        mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    // Integration test 4: Unauthenticated user cannot DELETE product — returns 401
    @Test
    void deleteProduct_withoutAuth_returns401() throws Exception {
        mockMvc.perform(delete("/api/products/1"))
                .andExpect(status().isUnauthorized());
    }

    // Integration test 5: SELLER cannot delete another seller's product — returns 403
    @Test
    void deleteProduct_byDifferentSeller_returns403() throws Exception {
        String ownerToken = createUserAndGetToken("owner@test.com", "Owner", RoleName.SELLER);
        String otherToken = createUserAndGetToken("other@test.com", "Other", RoleName.SELLER);

        // owner creates product
        ProductRequest req = new ProductRequest();
        req.setTitle("Owner Product");
        req.setPrice(BigDecimal.valueOf(15.00));
        req.setStock(5);

        String response = mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long productId = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(response).get("id").asLong();

        // other seller tries to delete
        mockMvc.perform(delete("/api/products/" + productId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden());
    }
}
