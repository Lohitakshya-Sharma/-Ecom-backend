package com.ecommerce;

import com.ecommerce.dto.ProductResponseDTO;
import com.ecommerce.entity.Product;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.service.ProductService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;

import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class ProductSearchIntegrationTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();

        productRepository.save(createProduct(
                "iPhone 17",
                "Apple smartphone",
                "79999.00",
                true
        ));

        productRepository.save(createProduct(
                "Samsung Galaxy",
                "Android smartphone",
                "59999.00",
                true
        ));

        productRepository.save(createProduct(
                "Old iPhone",
                "Discontinued Apple smartphone",
                "29999.00",
                false
        ));

        productRepository.save(createProduct(
                "Wireless Keyboard",
                "Bluetooth computer accessory",
                "2499.00",
                true
        ));
    }

    @Test
    void shouldSearchProductsByNameIgnoringCase() {
        Page<ProductResponseDTO> result =
                productService.searchProducts(
                        "IPHONE", null, 0, 10, "name", "asc"
                );

        assertEquals(2, result.getTotalElements());
        assertTrue(result.getContent().stream()
                .allMatch(product -> product.getName()
                        .toLowerCase().contains("iphone")));
    }

    @Test
    void shouldSearchProductsByDescription() {
        Page<ProductResponseDTO> result =
                productService.searchProducts(
                        "bluetooth", null, 0, 10, "name", "asc"
                );

        assertEquals(1, result.getTotalElements());
        assertEquals(
                "Wireless Keyboard",
                result.getContent().get(0).getName()
        );
    }

    @Test
    void shouldFilterProductsByActiveStatus() {
        Page<ProductResponseDTO> activeProducts =
                productService.searchProducts(
                        null, true, 0, 10, "id", "asc"
                );

        Page<ProductResponseDTO> inactiveProducts =
                productService.searchProducts(
                        null, false, 0, 10, "id", "asc"
                );

        assertEquals(3, activeProducts.getTotalElements());
        assertEquals(1, inactiveProducts.getTotalElements());

        assertTrue(activeProducts.getContent().stream()
                .allMatch(ProductResponseDTO::getActive));

        assertTrue(inactiveProducts.getContent().stream()
                .noneMatch(ProductResponseDTO::getActive));
    }

    @Test
    void shouldCombineSearchAndActiveFilter() {
        Page<ProductResponseDTO> result =
                productService.searchProducts(
                        "iphone", true, 0, 10, "name", "asc"
                );

        assertEquals(1, result.getTotalElements());
        assertEquals("iPhone 17", result.getContent().get(0).getName());
    }

    @Test
    void shouldApplyPagination() {
        Page<ProductResponseDTO> firstPage =
                productService.searchProducts(
                        null, null, 0, 2, "name", "asc"
                );

        Page<ProductResponseDTO> secondPage =
                productService.searchProducts(
                        null, null, 1, 2, "name", "asc"
                );

        assertEquals(2, firstPage.getContent().size());
        assertEquals(2, secondPage.getContent().size());
        assertEquals(4, firstPage.getTotalElements());
        assertEquals(2, firstPage.getTotalPages());

        assertNotEquals(
                firstPage.getContent().get(0).getName(),
                secondPage.getContent().get(0).getName()
        );
    }

    @Test
    void shouldSortProductsByPriceDescending() {
        Page<ProductResponseDTO> result =
                productService.searchProducts(
                        null, null, 0, 10, "price", "desc"
                );

        assertEquals(4, result.getTotalElements());

        assertEquals(
                0,
                new BigDecimal("79999.00").compareTo(
                        result.getContent().get(0).getPrice()
                )
        );

        assertEquals(
                0,
                new BigDecimal("2499.00").compareTo(
                        result.getContent().get(3).getPrice()
                )
        );
    }

    private Product createProduct(
            String name,
            String description,
            String price,
            boolean active
    ) {
        Product product = new Product();
        product.setName(name);
        product.setDescription(description);
        product.setPrice(new BigDecimal(price));
        product.setActive(active);
        return product;
    }
}