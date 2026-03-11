package com.kuet.minimarket.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kuet.minimarket.dto.OrderItemRequest;
import com.kuet.minimarket.dto.OrderItemResponse;
import com.kuet.minimarket.dto.OrderRequest;
import com.kuet.minimarket.dto.OrderResponse;
import com.kuet.minimarket.entity.Order;
import com.kuet.minimarket.entity.OrderItem;
import com.kuet.minimarket.entity.OrderStatus;
import com.kuet.minimarket.entity.Product;
import com.kuet.minimarket.entity.ProductStatus;
import com.kuet.minimarket.entity.User;
import com.kuet.minimarket.exception.ForbiddenException;
import com.kuet.minimarket.exception.InsufficientStockException;
import com.kuet.minimarket.exception.ResourceNotFoundException;
import com.kuet.minimarket.repository.OrderRepository;
import com.kuet.minimarket.repository.ProductRepository;
import com.kuet.minimarket.security.CustomUserDetails;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    @Transactional
    public OrderResponse placeOrder(OrderRequest request, CustomUserDetails userDetails) {
        User buyer = userDetails.getUser();

        List<OrderItem> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (OrderItemRequest itemReq : request.getItems()) {
            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Product not found with id: " + itemReq.getProductId()));

            if (product.getStatus() != ProductStatus.ACTIVE) {
                throw new InsufficientStockException(
                        "Product is not available for purchase: " + product.getTitle());
            }

            // Buyer cannot purchase their own product
            if (product.getSeller().getId().equals(buyer.getId())) {
                throw new ForbiddenException(
                        "You cannot buy your own product: '" + product.getTitle() + "'");
            }

            if (product.getStock() < itemReq.getQuantity()) {
                throw new InsufficientStockException(
                        "Insufficient stock for '" + product.getTitle() +
                        "'. Available: " + product.getStock() +
                        ", requested: " + itemReq.getQuantity());
            }

            // Stock is NOT deducted here — deducted only when seller marks COMPLETED
            BigDecimal lineTotal = product.getPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            total = total.add(lineTotal);

            items.add(OrderItem.builder()
                    .product(product)
                    .quantity(itemReq.getQuantity())
                    .unitPrice(product.getPrice()) // price at purchase time
                    .build());
        }

        Order order = Order.builder()
                .buyer(buyer)
                .totalAmount(total)
                .status(OrderStatus.PLACED)
                .build();

        // Link items to the order
        items.forEach(item -> item.setOrder(order));
        order.getItems().addAll(items);

        return toResponse(orderRepository.save(order));
    }

    public List<OrderResponse> getMyOrders(CustomUserDetails userDetails) {
        return orderRepository.findByBuyerId(userDetails.getUser().getId()).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<OrderResponse> getSalesOrders(CustomUserDetails userDetails) {
        return orderRepository.findOrdersBySellerProductsId(userDetails.getUser().getId()).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public OrderResponse completeOrder(Long id, CustomUserDetails userDetails) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
        if (order.getStatus() != OrderStatus.PLACED) {
            throw new ForbiddenException("Only PLACED orders can be marked complete.");
        }
        Long sellerId = userDetails.getUser().getId();
        boolean hasSellersProducts = order.getItems().stream()
                .anyMatch(item -> item.getProduct().getSeller().getId().equals(sellerId));
        if (!hasSellersProducts) {
            throw new ForbiddenException("This order does not contain your products.");
        }
        // Deduct stock now that order is confirmed complete
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            int newStock = product.getStock() - item.getQuantity();
            if (newStock < 0) newStock = 0;
            product.setStock(newStock);
            if (newStock == 0) {
                product.setStatus(ProductStatus.SOLD_OUT);
            }
            productRepository.save(product);
        }
        order.setStatus(OrderStatus.COMPLETED);
        return toResponse(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse cancelOrder(Long id, CustomUserDetails userDetails) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new ForbiddenException("Order is already cancelled.");
        }
        Long userId = userDetails.getUser().getId();
        boolean isOrderBuyer = order.getBuyer().getId().equals(userId);
        boolean hasSellersProducts = order.getItems().stream()
                .anyMatch(item -> item.getProduct().getSeller().getId().equals(userId));

        String cancelledBy;
        if (isOrderBuyer) {
            // The buyer who placed the order is cancelling
            cancelledBy = "BUYER";
        } else if (hasSellersProducts) {
            // The seller whose product is in this order is cancelling
            cancelledBy = "SELLER";
        } else {
            throw new ForbiddenException("You are not authorized to cancel this order.");
        }

        // No stock to restore — stock is only deducted on COMPLETED, not on PLACED
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledBy(cancelledBy);
        return toResponse(orderRepository.save(order));
    }

    private OrderResponse toResponse(Order order) {
        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(item -> OrderItemResponse.builder()
                        .id(item.getId())
                        .productId(item.getProduct().getId())
                        .productTitle(item.getProduct().getTitle())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .build())
                .collect(Collectors.toList());

        return OrderResponse.builder()
                .id(order.getId())
                .buyerId(order.getBuyer().getId())
                .buyerName(order.getBuyer().getFullName())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .cancelledBy(order.getCancelledBy())
                .createdAt(order.getCreatedAt())
                .items(itemResponses)
                .build();
    }
}
