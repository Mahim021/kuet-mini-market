package com.kuet.minimarket.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.kuet.minimarket.entity.ProductStatus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProductResponse {

    private Long id;
    private String title;
    private String description;
    private BigDecimal price;
    private Integer stock;
    private String imageUrl;
    private ProductStatus status;
    private Long sellerId;
    private String sellerName;
    private Instant createdAt;
    private Instant updatedAt;
}
