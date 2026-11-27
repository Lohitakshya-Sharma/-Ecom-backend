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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


class OrderIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private OrderRepository orderRepository;

    @BeforeEach
    void cleanDatabase() {
        orderRepository.deleteAll();
        productRepository.deleteAll();
    }

    @Test
    void shouldCreateOrderAndDeductInventory() {

        // Arrange: Create inventory and product together (cascade persists inventory)
        Inventory inventory = new Inventory();
        inventory.setStock(10);

        Product product = new Product();
        product.setName("Test Product");
        product.setDescription("Integration test product");
        product.setPrice(new BigDecimal("100.00"));
        product.setActive(true);
        product.setInventory(inventory);

        Product savedProduct = productRepository.save(product);
        Long inventoryId = savedProduct.getInventory().getId();

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

        Inventory updatedInventory = inventoryRepository
                .findById(inventoryId)
                .orElseThrow();

        assertEquals(8, updatedInventory.getStock());
    }
}
