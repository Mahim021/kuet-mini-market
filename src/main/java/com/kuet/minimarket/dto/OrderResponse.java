package com.kuet.minimarket.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.kuet.minimarket.entity.OrderStatus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrderResponse {

    private Long id;
    private Long buyerId;
    private String buyerName;
    private BigDecimal totalAmount;
    private OrderStatus status;
    private String cancelledBy;
    private Instant createdAt;
    private List<OrderItemResponse> items;
}
