package com.ecommerce.service;

import com.ecommerce.dto.CreateOrderRequestDTO;
import com.ecommerce.dto.OrderItemRequestDTO;
import com.ecommerce.dto.OrderResponseDTO;
import com.ecommerce.entity.*;
import com.ecommerce.exception.InsufficientStockException;
import com.ecommerce.exception.OrderNotFoundException;
import com.ecommerce.exception.ProductNotFoundException;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.ProductRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private OrderService orderService;

    @Test
    void createOrder_shouldCreateOrderAndDeductStock() {
        // Arrange
        Product product = createProduct(1L, "Test Product", "100.00", 10);

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> {
                    Order order = invocation.getArgument(0);
                    order.setId(1L);
                    return order;
                });

        CreateOrderRequestDTO request = createOrderRequest(1L, 2);

        // Act
        OrderResponseDTO response = orderService.createOrder(request);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(OrderStatus.PENDING, response.getStatus());
        assertEquals(new BigDecimal("200.00"), response.getTotalAmount());
        assertEquals(1, response.getOrderItems().size());
        assertEquals(8, product.getInventory().getStock());

        assertEquals(2, response.getOrderItems().get(0).getQuantity());

        assertEquals(
                new BigDecimal("100.00"),
                response.getOrderItems().get(0).getUnitPrice()
        );

        assertEquals(
                new BigDecimal("200.00"),
                response.getOrderItems().get(0).getSubtotal()
        );

        assertEquals(
                1L,
                response.getOrderItems().get(0).getProductId()
        );

        assertEquals(
                "Test Product",
                response.getOrderItems().get(0).getProductName()
        );

        verify(productRepository).findById(1L);
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void createOrder_shouldThrowWhenStockIsInsufficient() {
        Product product = createProduct(1L, "Test Product", "100.00", 2);

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        CreateOrderRequestDTO request = createOrderRequest(1L, 5);

        assertThrows(
                InsufficientStockException.class,
                () -> orderService.createOrder(request)
        );

        assertEquals(2, product.getInventory().getStock());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void createOrder_shouldRejectDuplicateProductIds() {
        Product product = createProduct(1L, "Test Product", "100.00", 10);

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        CreateOrderRequestDTO request = new CreateOrderRequestDTO();
        request.setItems(List.of(
                createOrderItemRequest(1L, 1),
                createOrderItemRequest(1L, 2)
        ));

        assertThrows(
                IllegalArgumentException.class,
                () -> orderService.createOrder(request)
        );

        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void createOrder_shouldThrowWhenProductDoesNotExist() {
        when(productRepository.findById(99L))
                .thenReturn(Optional.empty());

        CreateOrderRequestDTO request = createOrderRequest(99L, 1);

        assertThrows(
                ProductNotFoundException.class,
                () -> orderService.createOrder(request)
        );

        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void createOrder_shouldRejectInactiveProduct() {
        Product product = createProduct(
                1L,
                "Inactive Product",
                "100.00",
                10
        );

        product.setActive(false);

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        CreateOrderRequestDTO request = createOrderRequest(1L, 2);

        assertThrows(
                IllegalArgumentException.class,
                () -> orderService.createOrder(request)
        );

        assertEquals(10, product.getInventory().getStock());

        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void getOrderById_shouldThrowWhenOrderDoesNotExist() {
        when(orderRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                OrderNotFoundException.class,
                () -> orderService.getOrderById(99L)
        );
    }

    @Test
    void getOrderById_shouldReturnOrderWhenItExists() {
        Order order = new Order();
        order.setId(1L);
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(new BigDecimal("200.00"));

        when(orderRepository.findById(1L))
                .thenReturn(Optional.of(order));

        OrderResponseDTO response = orderService.getOrderById(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(OrderStatus.PENDING, response.getStatus());
        assertEquals(new BigDecimal("200.00"), response.getTotalAmount());

        verify(orderRepository).findById(1L);
    }

    @Test
    void cancelOrder_shouldThrowWhenOrderDoesNotExist() {
        when(orderRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                OrderNotFoundException.class,
                () -> orderService.cancelOrder(99L)
        );

        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void cancelOrder_shouldRestoreStockAndMarkOrderCancelled() {
        Product product = createProduct(1L, "Test Product", "100.00", 8);

        Order order = new Order();
        order.setId(1L);
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(new BigDecimal("200.00"));

        OrderItem item = new OrderItem();
        item.setProduct(product);
        item.setQuantity(2);
        item.setUnitPrice(new BigDecimal("100.00"));

        order.addOrderItem(item);

        when(orderRepository.findById(1L))
                .thenReturn(Optional.of(order));

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponseDTO response = orderService.cancelOrder(1L);

        assertEquals(OrderStatus.CANCELLED, response.getStatus());
        assertEquals(10, product.getInventory().getStock());

        verify(orderRepository).save(order);
    }

    @Test
    void cancelOrder_shouldRejectAlreadyCancelledOrderWithoutRestoringStock() {
        Product product = createProduct(1L, "Test Product", "100.00", 8);

        Order order = new Order();
        order.setId(1L);
        order.setStatus(OrderStatus.CANCELLED);

        OrderItem item = new OrderItem();
        item.setProduct(product);
        item.setQuantity(2);
        item.setUnitPrice(new BigDecimal("100.00"));

        order.addOrderItem(item);

        when(orderRepository.findById(1L))
                .thenReturn(Optional.of(order));

        assertThrows(
                IllegalStateException.class,
                () -> orderService.cancelOrder(1L)
        );

        assertEquals(8, product.getInventory().getStock());

        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void createOrder_shouldRejectProductWithoutInventory() {
        Product product = new Product();
        product.setId(1L);
        product.setName("Product Without Inventory");
        product.setPrice(new BigDecimal("100.00"));
        product.setActive(true);
        product.setInventory(null);

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        CreateOrderRequestDTO request = createOrderRequest(1L, 1);

        assertThrows(
                IllegalStateException.class,
                () -> orderService.createOrder(request)
        );

        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void cancelOrder_shouldRejectConfirmedOrder() {
        Order order = new Order();
        order.setId(1L);
        order.setStatus(OrderStatus.CONFIRMED);

        when(orderRepository.findById(1L))
                .thenReturn(Optional.of(order));

        assertThrows(
                IllegalStateException.class,
                () -> orderService.cancelOrder(1L)
        );

        verify(orderRepository, never()).save(any(Order.class));
    }

    private Product createProduct(
            Long id,
            String name,
            String price,
            int stock
    ) {
        Inventory inventory = new Inventory();
        inventory.setStock(stock);

        Product product = new Product();
        product.setId(id);
        product.setName(name);
        product.setPrice(new BigDecimal(price));
        product.setActive(true);
        product.setInventory(inventory);

        return product;
    }

    private CreateOrderRequestDTO createOrderRequest(
            Long productId,
            int quantity
    ) {
        CreateOrderRequestDTO request = new CreateOrderRequestDTO();
        request.setItems(List.of(
                createOrderItemRequest(productId, quantity)
        ));
        return request;
    }

    private OrderItemRequestDTO createOrderItemRequest(
            Long productId,
            int quantity
    ) {
        OrderItemRequestDTO item = new OrderItemRequestDTO();
        item.setProductId(productId);
        item.setQuantity(quantity);
        return item;
    }
}
