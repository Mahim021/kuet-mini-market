package com.kuet.minimarket.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuet.minimarket.dto.OrderItemRequest;
import com.kuet.minimarket.dto.OrderRequest;
import com.kuet.minimarket.entity.*;
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
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class OrderControllerIntegrationTest {

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

    // Integration test 4: BUYER can place an order — returns 201
    @Test
    void placeOrder_asBuyer_returns201WithOrderDetails() throws Exception {
        User seller = createAndSaveUser("seller@order.com", "Seller", RoleName.SELLER);
        createAndSaveUser("buyer@order.com", "Buyer", RoleName.BUYER);

        Product product = productRepository.save(Product.builder()
                .title("Keyboard").description("Mechanical keyboard")
                .price(BigDecimal.valueOf(50.00)).stock(10)
                .imageUrl("http://img.test").status(ProductStatus.ACTIVE)
                .seller(seller).build());

        OrderItemRequest itemReq = new OrderItemRequest();
        itemReq.setProductId(product.getId());
        itemReq.setQuantity(2);

        OrderRequest orderReq = new OrderRequest();
        orderReq.setItems(List.of(itemReq));

        String buyerToken = getToken("buyer@order.com");

        mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalAmount").value(100.00))
                .andExpect(jsonPath("$.status").value("PLACED"))
                .andExpect(jsonPath("$.items[0].productTitle").value("Keyboard"));
    }

    // Integration test 5: BUYER can view their own orders — returns 200
    @Test
    void getMyOrders_asBuyer_returns200() throws Exception {
        createAndSaveUser("buyer2@order.com", "Buyer2", RoleName.BUYER);
        String buyerToken = getToken("buyer2@order.com");

        mockMvc.perform(get("/api/orders/my")
                        .header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isOk());
    }

    // Integration test 6: SELLER cannot place an order — returns 403
    @Test
    void placeOrder_asSeller_returns403() throws Exception {
        User seller = createAndSaveUser("seller2@order.com", "Seller2", RoleName.SELLER);

        Product product = productRepository.save(Product.builder()
                .title("Monitor").price(BigDecimal.valueOf(200.00)).stock(5)
                .imageUrl("http://img.test").status(ProductStatus.ACTIVE)
                .seller(seller).build());

        OrderItemRequest itemReq = new OrderItemRequest();
        itemReq.setProductId(product.getId());
        itemReq.setQuantity(1);

        OrderRequest orderReq = new OrderRequest();
        orderReq.setItems(List.of(itemReq));

        String sellerToken = getToken("seller2@order.com");

        mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + sellerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderReq)))
                .andExpect(status().isForbidden());
    }

    // Integration test 7: ADMIN can view all orders — returns 200
    @Test
    void getAllOrders_asAdmin_returns200() throws Exception {
        createAndSaveUser("admin@orders.com", "Admin", RoleName.ADMIN);
        String adminToken = getToken("admin@orders.com");

        mockMvc.perform(get("/api/orders")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // Integration test 8: BUYER cannot view all orders (admin-only) — returns 403
    @Test
    void getAllOrders_asBuyer_returns403() throws Exception {
        createAndSaveUser("buyer3@order.com", "Buyer3", RoleName.BUYER);
        String buyerToken = getToken("buyer3@order.com");

        mockMvc.perform(get("/api/orders")
                        .header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isForbidden());
    }
}
