package com.kuet.minimarket.service;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kuet.minimarket.dto.OrderItemRequest;
import com.kuet.minimarket.dto.OrderRequest;
import com.kuet.minimarket.dto.OrderResponse;
import com.kuet.minimarket.entity.Order;
import com.kuet.minimarket.entity.OrderItem;
import com.kuet.minimarket.entity.OrderStatus;
import com.kuet.minimarket.entity.Product;
import com.kuet.minimarket.entity.ProductStatus;
import com.kuet.minimarket.entity.Role;
import com.kuet.minimarket.entity.RoleName;
import com.kuet.minimarket.entity.User;
import com.kuet.minimarket.exception.InsufficientStockException;
import com.kuet.minimarket.exception.ResourceNotFoundException;
import com.kuet.minimarket.repository.OrderRepository;
import com.kuet.minimarket.repository.ProductRepository;
import com.kuet.minimarket.security.CustomUserDetails;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private ProductRepository productRepository;
    @InjectMocks private OrderService orderService;

    private User seller;
    private User buyer;
    private Product product;
    private CustomUserDetails sellerDetails;
    private CustomUserDetails buyerDetails;

    @BeforeEach
    void setUp() {
        Set<Role> sellerRoles = new HashSet<>(Set.of(new Role(1L, RoleName.SELLER)));
        seller = User.builder()
                .id(1L).fullName("Seller").email("seller@test.com")
                .password("enc").enabled(true).roles(sellerRoles).build();

        Set<Role> buyerRoles = new HashSet<>(Set.of(new Role(2L, RoleName.BUYER)));
        buyer = User.builder()
                .id(3L).fullName("Buyer").email("buyer@test.com")
                .password("enc").enabled(true).roles(buyerRoles).build();

        sellerDetails = new CustomUserDetails(seller);
        buyerDetails = new CustomUserDetails(buyer);

        product = Product.builder()
                .id(1L).title("Laptop").description("Gaming Laptop")
                .price(BigDecimal.valueOf(10.00)).stock(5)
                .imageUrl("http://img.url").status(ProductStatus.ACTIVE)
                .seller(seller).build();
    }

    // Test 6: placeOrder success — calculates total correctly, does NOT deduct stock
    @Test
    void placeOrder_success_calculatesTotal() {
        OrderItemRequest itemReq = new OrderItemRequest();
        itemReq.setProductId(1L);
        itemReq.setQuantity(2);

        OrderRequest request = new OrderRequest();
        request.setItems(List.of(itemReq));

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse result = orderService.placeOrder(request, buyerDetails);

        assertThat(result.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(20.00));
        assertThat(result.getBuyerId()).isEqualTo(3L);
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getQuantity()).isEqualTo(2);
        assertThat(product.getStock()).isEqualTo(5); // stock NOT deducted at place time
        verify(productRepository, never()).save(any()); // product never saved in placeOrder
    }

    // Test 7: placeOrder throws when stock insufficient
    @Test
    void placeOrder_insufficientStock_throwsInsufficientStockException() {
        OrderItemRequest itemReq = new OrderItemRequest();
        itemReq.setProductId(1L);
        itemReq.setQuantity(10); // stock is only 5

        OrderRequest request = new OrderRequest();
        request.setItems(List.of(itemReq));

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> orderService.placeOrder(request, buyerDetails))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Laptop");
        verify(orderRepository, never()).save(any());
    }

    // Test 8: placeOrder throws when product not found
    @Test
    void placeOrder_productNotFound_throwsResourceNotFoundException() {
        OrderItemRequest itemReq = new OrderItemRequest();
        itemReq.setProductId(999L);
        itemReq.setQuantity(1);

        OrderRequest request = new OrderRequest();
        request.setItems(List.of(itemReq));

        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.placeOrder(request, buyerDetails))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    // Test 9: getMyOrders returns buyer's own orders
    @Test
    void getMyOrders_returnsOnlyBuyersOrders() {
        OrderItem item = OrderItem.builder()
                .id(10L).product(product).quantity(1)
                .unitPrice(BigDecimal.valueOf(10.00)).build();
        Order order = Order.builder()
                .id(100L).buyer(buyer).totalAmount(BigDecimal.valueOf(10.00))
                .status(OrderStatus.PLACED).build();
        order.getItems().add(item);
        item.setOrder(order);

        when(orderRepository.findByBuyerId(3L)).thenReturn(List.of(order));

        List<OrderResponse> result = orderService.getMyOrders(buyerDetails);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getBuyerId()).isEqualTo(3L);
        assertThat(result.get(0).getItems()).hasSize(1);
    }

    // Test 10: getSalesOrders returns orders containing seller's products
    @Test
    void getSalesOrders_returnsOrdersContainingSellerProducts() {
        OrderItem item = OrderItem.builder()
                .id(20L).product(product).quantity(2)
                .unitPrice(BigDecimal.valueOf(10.00)).build();
        Order order = Order.builder()
                .id(200L).buyer(buyer).totalAmount(BigDecimal.valueOf(20.00))
                .status(OrderStatus.PLACED).build();
        order.getItems().add(item);
        item.setOrder(order);

        when(orderRepository.findOrdersBySellerProductsId(1L)).thenReturn(List.of(order));

        List<OrderResponse> result = orderService.getSalesOrders(sellerDetails);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getItems().get(0).getProductId()).isEqualTo(1L);
    }

    // Test 11: placeOrder does NOT touch stock — stock deducted only on completeOrder
    @Test
    void placeOrder_doesNotDeductStock() {
        product.setStock(2); // exactly 2 left

        OrderItemRequest itemReq = new OrderItemRequest();
        itemReq.setProductId(1L);
        itemReq.setQuantity(2);

        OrderRequest request = new OrderRequest();
        request.setItems(List.of(itemReq));

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        orderService.placeOrder(request, buyerDetails);

        assertThat(product.getStock()).isEqualTo(2); // stock unchanged after place
        assertThat(product.getStatus()).isEqualTo(ProductStatus.ACTIVE); // still ACTIVE
        verify(productRepository, never()).save(any()); // product never saved in placeOrder
    }

    // Test 12: placeOrder on REMOVED product throws InsufficientStockException
    @Test
    void placeOrder_removedProduct_throwsInsufficientStockException() {
        product.setStatus(ProductStatus.REMOVED);

        OrderItemRequest itemReq = new OrderItemRequest();
        itemReq.setProductId(1L);
        itemReq.setQuantity(1);

        OrderRequest request = new OrderRequest();
        request.setItems(List.of(itemReq));

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> orderService.placeOrder(request, buyerDetails))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("not available");
        verify(orderRepository, never()).save(any());
    }

    // Test 13: getAllOrders returns every order
    @Test
    void getAllOrders_returnsAllOrders() {
        OrderItem item = OrderItem.builder()
                .id(30L).product(product).quantity(1)
                .unitPrice(BigDecimal.valueOf(10.00)).build();
        Order order = Order.builder()
                .id(300L).buyer(buyer).totalAmount(BigDecimal.valueOf(10.00))
                .status(OrderStatus.PLACED).build();
        order.getItems().add(item);
        item.setOrder(order);

        when(orderRepository.findAll()).thenReturn(List.of(order));

        List<OrderResponse> result = orderService.getAllOrders();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(300L);
    }
}
