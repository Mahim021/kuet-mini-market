package com.kuet.minimarket.repository;

import com.kuet.minimarket.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    void deleteByProductId(Long productId);
}
