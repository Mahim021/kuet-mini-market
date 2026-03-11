package com.kuet.minimarket.service;

import com.kuet.minimarket.dto.ProductRequest;
import com.kuet.minimarket.dto.ProductResponse;
import com.kuet.minimarket.entity.*;
import com.kuet.minimarket.exception.ForbiddenException;
import com.kuet.minimarket.exception.ResourceNotFoundException;
import com.kuet.minimarket.repository.OrderItemRepository;
import com.kuet.minimarket.repository.ProductRepository;
import com.kuet.minimarket.security.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @InjectMocks private ProductService productService;

    private User seller;
    private CustomUserDetails sellerDetails;
    private Product product;

    @BeforeEach
    void setUp() {
        Set<Role> sellerRoles = new HashSet<>(Set.of(new Role(2L, RoleName.SELLER)));
        seller = User.builder()
                .id(1L).fullName("Test Seller").email("seller@test.com")
                .password("encoded").enabled(true).roles(sellerRoles).build();
        sellerDetails = new CustomUserDetails(seller);

        product = Product.builder()
                .id(1L).title("Test Product").description("Desc")
                .price(BigDecimal.valueOf(10.00)).stock(5)
                .imageUrl("http://img.url").status(ProductStatus.ACTIVE)
                .seller(seller).build();
    }

    // Test 1: getAllProducts returns full list
    @Test
    void getAllProducts_returnsMappedList() {
        when(productRepository.findAll()).thenReturn(List.of(product));

        List<ProductResponse> result = productService.getAllProducts();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Test Product");
        assertThat(result.get(0).getSellerId()).isEqualTo(1L);
        assertThat(result.get(0).getImageUrl()).isEqualTo("http://img.url");
    }

    // Test 2: getProductById returns response when found
    @Test
    void getProductById_found_returnsResponse() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        ProductResponse result = productService.getProductById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getStatus()).isEqualTo(ProductStatus.ACTIVE);
        assertThat(result.getSellerName()).isEqualTo("Test Seller");
    }

    // Test 3: getProductById throws when not found
    @Test
    void getProductById_notFound_throwsResourceNotFoundException() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // Test 4: createProduct saves product and returns response
    @Test
    void createProduct_asSeller_savesAndReturnsResponse() {
        ProductRequest req = new ProductRequest();
        req.setTitle("New Product");
        req.setPrice(BigDecimal.valueOf(20.00));
        req.setStock(10);
        req.setImageUrl("http://new.url");

        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductResponse result = productService.createProduct(req, sellerDetails);

        assertThat(result.getTitle()).isEqualTo("New Product");
        assertThat(result.getSellerId()).isEqualTo(1L);
        assertThat(result.getStatus()).isEqualTo(ProductStatus.ACTIVE);
        verify(productRepository).save(any(Product.class));
    }

    // Test 5: updateProduct throws ForbiddenException when called by different seller
    @Test
    void updateProduct_byDifferentSeller_throwsForbiddenException() {
        Set<Role> otherRoles = new HashSet<>(Set.of(new Role(2L, RoleName.SELLER)));
        User otherSeller = User.builder()
                .id(2L).email("other@test.com").enabled(true).roles(otherRoles).build();
        CustomUserDetails otherDetails = new CustomUserDetails(otherSeller);

        ProductRequest req = new ProductRequest();
        req.setTitle("Hacked Title");
        req.setPrice(BigDecimal.valueOf(1.00));
        req.setStock(0);

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> productService.updateProduct(1L, req, otherDetails))
                .isInstanceOf(ForbiddenException.class);
        verify(productRepository, never()).save(any());
    }

    // Test 6: ADMIN can update any product regardless of ownership
    @Test
    void updateProduct_byAdmin_succeedsRegardlessOfOwnership() {
        Set<Role> adminRoles = new HashSet<>(Set.of(new Role(1L, RoleName.ADMIN)));
        User admin = User.builder()
                .id(99L).email("admin@test.com").enabled(true).roles(adminRoles).build();
        CustomUserDetails adminDetails = new CustomUserDetails(admin);

        ProductRequest req = new ProductRequest();
        req.setTitle("Admin Updated");
        req.setPrice(BigDecimal.valueOf(50.00));
        req.setStock(20);

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductResponse result = productService.updateProduct(1L, req, adminDetails);

        assertThat(result.getTitle()).isEqualTo("Admin Updated");
        verify(productRepository).save(any(Product.class));
    }

    // Test 7: deleteProduct by owner succeeds
    @Test
    void deleteProduct_byOwner_succeeds() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        productService.deleteProduct(1L, sellerDetails);

        verify(orderItemRepository).deleteByProductId(1L);
        verify(productRepository).delete(product);
    }

    // Test 8: deleteProduct by non-owner throws ForbiddenException
    @Test
    void deleteProduct_byNonOwner_throwsForbiddenException() {
        Set<Role> otherRoles = new HashSet<>(Set.of(new Role(2L, RoleName.SELLER)));
        User otherSeller = User.builder()
                .id(99L).email("notowner@test.com").enabled(true).roles(otherRoles).build();
        CustomUserDetails otherDetails = new CustomUserDetails(otherSeller);

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> productService.deleteProduct(1L, otherDetails))
                .isInstanceOf(ForbiddenException.class);
        verify(productRepository, never()).delete(any());
    }
}
