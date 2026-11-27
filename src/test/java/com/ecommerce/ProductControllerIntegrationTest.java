package com.ecommerce;

import com.ecommerce.dto.ProductRequestDTO;
import com.ecommerce.dto.ProductResponseDTO;
import com.ecommerce.entity.Inventory;
import com.ecommerce.entity.Product;
import com.ecommerce.repository.InventoryRepository;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


class ProductControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private OrderRepository orderRepository;

    private MockMvc mockMvc;
    private Long productId;
    private Long inventoryId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        orderRepository.deleteAll();
        productRepository.deleteAll();
        inventoryRepository.deleteAll();

        ProductResponseDTO created = productService.createProduct(new ProductRequestDTO(
                "iPhone 17",
                "Apple smartphone",
                new BigDecimal("79999.00"),
                true,
                10
        ));
        productId = created.getId();
        Product savedProduct = productRepository.findById(productId).orElseThrow();
        inventoryId = savedProduct.getInventory().getId();
    }

    @Test
    void updateProduct_shouldUpdateMetadataWithoutChangingStock() throws Exception {
        String body = """
                {
                  "name": "iPhone 17 Pro",
                  "description": "Updated Apple smartphone",
                  "price": 99999.99,
                  "active": false
                }
                """;

        mockMvc.perform(put("/api/v1/products/{id}", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(productId))
                .andExpect(jsonPath("$.name").value("iPhone 17 Pro"))
                .andExpect(jsonPath("$.description").value("Updated Apple smartphone"))
                .andExpect(jsonPath("$.price").value(99999.99))
                .andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$.stock").value(10));

        Inventory inventory = inventoryRepository.findById(inventoryId).orElseThrow();
        assertEquals(10, inventory.getStock());
    }

    @Test
    void adjustStock_shouldAdjustStockAndIncrementVersion() throws Exception {
        Long initialVersion = inventoryRepository.findById(inventoryId).orElseThrow().getVersion();

        mockMvc.perform(patch("/api/v1/products/{id}/stock", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"delta\": 5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(productId))
                .andExpect(jsonPath("$.stock").value(15));

        Inventory afterIncrease = inventoryRepository.findById(inventoryId).orElseThrow();
        assertEquals(15, afterIncrease.getStock());
        assertEquals(initialVersion + 1, afterIncrease.getVersion());

        mockMvc.perform(patch("/api/v1/products/{id}/stock", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"delta\": -3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(12));

        Inventory afterDecrease = inventoryRepository.findById(inventoryId).orElseThrow();
        assertEquals(12, afterDecrease.getStock());
        assertEquals(initialVersion + 2, afterDecrease.getVersion());
    }

    @Test
    void adjustStock_shouldReturnConflictWhenNewStockIsNegative() throws Exception {
        Long initialVersion = inventoryRepository.findById(inventoryId).orElseThrow().getVersion();

        mockMvc.perform(patch("/api/v1/products/{id}/stock", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"delta\": -11}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Insufficient Stock"));

        Inventory unchanged = inventoryRepository.findById(inventoryId).orElseThrow();
        assertEquals(10, unchanged.getStock());
        assertEquals(initialVersion, unchanged.getVersion());
    }

    @Test
    void adjustStock_shouldReturnBadRequestWhenDeltaIsNull() throws Exception {
        mockMvc.perform(patch("/api/v1/products/{id}/stock", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
