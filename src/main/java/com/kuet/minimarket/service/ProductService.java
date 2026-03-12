package com.kuet.minimarket.service;

import com.kuet.minimarket.dto.ProductRequest;
import com.kuet.minimarket.dto.ProductResponse;
import com.kuet.minimarket.entity.Product;
import com.kuet.minimarket.entity.ProductStatus;
import com.kuet.minimarket.entity.User;
import com.kuet.minimarket.exception.ForbiddenException;
import com.kuet.minimarket.exception.ResourceNotFoundException;
import com.kuet.minimarket.repository.OrderItemRepository;
import com.kuet.minimarket.repository.ProductRepository;
import com.kuet.minimarket.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;

    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<ProductResponse> getMyProducts(CustomUserDetails userDetails) {
        Long sellerId = userDetails.getUser().getId();
        return productRepository.findBySellerId(sellerId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public ProductResponse getProductById(Long id) {
        return toResponse(findProductOrThrow(id));
    }

    @Transactional
    public ProductResponse createProduct(ProductRequest request, CustomUserDetails userDetails) {
        User seller = userDetails.getUser();
        Product product = Product.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .price(request.getPrice())
                .stock(request.getStock())
                .imageUrl(request.getImageUrl())
                .status(request.getStatus() != null ? request.getStatus() : ProductStatus.ACTIVE)
                .seller(seller)
                .build();
        return toResponse(productRepository.save(product));
    }

    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request, CustomUserDetails userDetails) {
        Product product = findProductOrThrow(id);
        checkOwnershipOrAdmin(product, userDetails);

        product.setTitle(request.getTitle());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setImageUrl(request.getImageUrl());
        if (request.getStatus() != null) {
            product.setStatus(request.getStatus());
        }
        return toResponse(productRepository.save(product));
    }

    @Transactional
    public void deleteProduct(Long id, CustomUserDetails userDetails) {
        Product product = findProductOrThrow(id);
        checkOwnershipOrAdmin(product, userDetails);
        orderItemRepository.deleteByProductId(id);
        productRepository.delete(product);
    }

    private Product findProductOrThrow(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
    }

    private void checkOwnershipOrAdmin(Product product, CustomUserDetails userDetails) {
        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin && !product.getSeller().getId().equals(userDetails.getUser().getId())) {
            throw new ForbiddenException("You do not have permission to modify this product");
        }
    }

    private ProductResponse toResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .title(product.getTitle())
                .description(product.getDescription())
                .price(product.getPrice())
                .stock(product.getStock())
                .imageUrl(product.getImageUrl())
                .status(product.getStatus())
                .sellerId(product.getSeller().getId())
                .sellerName(product.getSeller().getFullName())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}
