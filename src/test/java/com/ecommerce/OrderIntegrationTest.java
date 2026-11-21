package com.ecommerce;

import com.ecommerce.dto.CreateOrderRequestDTO;
import com.ecommerce.dto.OrderItemRequestDTO;
import com.ecommerce.dto.OrderResponseDTO;
import com.ecommerce.entity.Inventory;
import com.ecommerce.entity.OrderStatus;
import com.ecommerce.entity.Product;
import com.ecommerce.repository.InventoryRepository;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.service.OrderService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class OrderIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Test
    void shouldCreateOrderAndDeductInventory() {

        // Arrange: Create and persist inventory
        Inventory inventory = new Inventory();
        inventory.setStock(10);

        Inventory savedInventory = inventoryRepository.save(inventory);

        // Arrange: Create and persist product
        Product product = new Product();
        product.setName("Test Product");
        product.setDescription("Integration test product");
        product.setPrice(new BigDecimal("100.00"));
        product.setActive(true);
        product.setInventory(savedInventory);

        Product savedProduct = productRepository.save(product);

        // Arrange: Build order request using Record canonical constructor
        OrderItemRequestDTO itemRequest = new OrderItemRequestDTO(savedProduct.getId(), 2);

        CreateOrderRequestDTO request = new CreateOrderRequestDTO(
                "customer@example.com",
                "John Doe",
                "123 Main St, New York, NY",
                List.of(itemRequest)
        );

        // Act: Create the order through the real service
        OrderResponseDTO response = orderService.createOrder(request);

        // Assert: Verify order response
        assertNotNull(response.id());
        assertEquals("customer@example.com", response.customerEmail());
        assertEquals("John Doe", response.customerName());
        assertEquals("123 Main St, New York, NY", response.shippingAddress());
        assertEquals(OrderStatus.PENDING, response.status());
        assertEquals(new BigDecimal("200.00"), response.totalAmount());

        assertEquals(1, response.orderItems().size());
        assertEquals(2, response.orderItems().get(0).quantity());

        // Assert: Verify inventory deduction
        Inventory updatedInventory = inventoryRepository
                .findById(savedInventory.getId())
                .orElseThrow();

        assertEquals(8, updatedInventory.getStock());

        // Assert: Verify order persistence
        assertTrue(orderRepository.existsById(response.id()));
    }
}
