package com.ecommerce.service;

import com.ecommerce.dto.ProductRequestDTO;
import com.ecommerce.dto.ProductResponseDTO;
import com.ecommerce.dto.ProductUpdateRequestDTO;
import com.ecommerce.entity.Inventory;
import com.ecommerce.entity.Product;
import com.ecommerce.exception.InsufficientStockException;
import com.ecommerce.exception.InvalidRequestException;
import com.ecommerce.exception.ProductNotFoundException;
import com.ecommerce.mapper.ProductMapper;
import com.ecommerce.repository.InventoryRepository;
import com.ecommerce.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @Spy
    private ProductMapper productMapper = Mappers.getMapper(ProductMapper.class);

    @InjectMocks
    private ProductService productService;

    @Test
    void createProduct_shouldReturnProductResponse() {

        Product savedProduct = new Product();
        savedProduct.setId(1L);
        savedProduct.setName("iPhone 17");
        savedProduct.setDescription("Apple smartphone");
        savedProduct.setPrice(new BigDecimal("79999"));
        savedProduct.setActive(true);

        when(productRepository.save(any(Product.class)))
                .thenReturn(savedProduct);

        ProductRequestDTO dto = new ProductRequestDTO(
                "iPhone 17",
                "Apple smartphone",
                new BigDecimal("79999"),
                true,
                10
        );

        when(inventoryRepository.save(any(Inventory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponseDTO response = productService.createProduct(dto);

        assertEquals(1L, response.getId());
        assertEquals("iPhone 17", response.getName());
        assertEquals("Apple smartphone", response.getDescription());
        assertEquals(new BigDecimal("79999"), response.getPrice());
        assertEquals(true, response.getActive());

        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    void getProductById_shouldThrowWhenProductDoesNotExist() {

        when(productRepository.findById(999L))
                .thenReturn(Optional.empty());

        ProductNotFoundException exception = assertThrows(
                ProductNotFoundException.class,
                () -> productService.getProductById(999L)
        );

        assertEquals(
                "Product not found with id: 999",
                exception.getMessage()
        );
    }

    @Test
    void updateProduct_shouldUpdateFieldsOnlyWithoutChangingStock() {

        Inventory existingInventory = new Inventory();
        existingInventory.setStock(10);

        Product existingProduct = new Product();
        existingProduct.setId(1L);
        existingProduct.setName("iPhone 17");
        existingProduct.setDescription("Apple smartphone");
        existingProduct.setPrice(new BigDecimal("79999"));
        existingProduct.setActive(true);
        existingProduct.setInventory(existingInventory);

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(existingProduct));

        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProductUpdateRequestDTO dto = new ProductUpdateRequestDTO(
                "iPhone 17 Pro",
                "Updated Apple smartphone",
                new BigDecimal("99999"),
                false
        );

        ProductResponseDTO response =
                productService.updateProduct(1L, dto);

        assertEquals(1L, response.getId());
        assertEquals("iPhone 17 Pro", response.getName());
        assertEquals("Updated Apple smartphone", response.getDescription());
        assertEquals(new BigDecimal("99999"), response.getPrice());
        assertEquals(false, response.getActive());
        assertEquals(10, response.getStock());
        assertEquals(10, existingInventory.getStock());

        verify(productRepository).findById(1L);
        verify(productRepository).save(existingProduct);
        verifyNoInteractions(inventoryRepository);
    }

    @Test
    void adjustStock_shouldIncreaseStockAndSaveInventory() {
        Inventory existingInventory = new Inventory();
        existingInventory.setStock(10);

        Product existingProduct = createProduct(
                1L,
                "iPhone 17",
                "Apple smartphone",
                "79999",
                true
        );
        existingProduct.setInventory(existingInventory);

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(existingProduct));
        when(inventoryRepository.save(existingInventory))
                .thenReturn(existingInventory);

        ProductResponseDTO response = productService.adjustStock(1L, 5);

        assertEquals(15, existingInventory.getStock());
        assertEquals(15, response.getStock());
        verify(inventoryRepository).save(existingInventory);
    }

    @Test
    void adjustStock_shouldDecreaseStockToZeroAndSaveInventory() {
        Inventory existingInventory = new Inventory();
        existingInventory.setStock(10);

        Product existingProduct = createProduct(
                1L,
                "iPhone 17",
                "Apple smartphone",
                "79999",
                true
        );
        existingProduct.setInventory(existingInventory);

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(existingProduct));
        when(inventoryRepository.save(existingInventory))
                .thenReturn(existingInventory);

        ProductResponseDTO response = productService.adjustStock(1L, -10);

        assertEquals(0, existingInventory.getStock());
        assertEquals(0, response.getStock());
        verify(inventoryRepository).save(existingInventory);
    }

    @Test
    void adjustStock_shouldThrowInsufficientStockWhenNewStockIsNegative() {
        Inventory existingInventory = new Inventory();
        existingInventory.setStock(5);

        Product existingProduct = createProduct(
                1L,
                "iPhone 17",
                "Apple smartphone",
                "79999",
                true
        );
        existingProduct.setInventory(existingInventory);

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(existingProduct));

        assertThrows(
                InsufficientStockException.class,
                () -> productService.adjustStock(1L, -6)
        );

        assertEquals(5, existingInventory.getStock());
        verify(inventoryRepository, never()).save(any());
    }

    @Test
    void adjustStock_shouldThrowWhenProductDoesNotExist() {
        when(productRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                ProductNotFoundException.class,
                () -> productService.adjustStock(999L, 5)
        );

        verifyNoInteractions(inventoryRepository);
    }

    @Test
    void adjustStock_shouldThrowWhenInventoryIsMissing() {
        Product existingProduct = createProduct(
                1L,
                "iPhone 17",
                "Apple smartphone",
                "79999",
                true
        );

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(existingProduct));

        assertThrows(
                IllegalStateException.class,
                () -> productService.adjustStock(1L, 5)
        );

        verifyNoInteractions(inventoryRepository);
    }

    @Test
    void searchProducts_shouldReturnMappedProductsAndApplyPaginationAndSorting() {

        Product product = createProduct(
                1L,
                "iPhone 17",
                "Apple smartphone",
                "79999",
                true
        );

        Page<Product> productPage =
                new PageImpl<>(List.of(product));

        when(productRepository.searchProducts(
                eq("iphone"),
                eq(true),
                any(Pageable.class)
        )).thenReturn(productPage);

        Page<ProductResponseDTO> response = productService.searchProducts(
                "iphone",
                true,
                1,
                5,
                "price",
                "desc"
        );

        assertEquals(1, response.getTotalElements());
        assertEquals(1, response.getContent().size());

        ProductResponseDTO result = response.getContent().get(0);

        assertEquals(1L, result.getId());
        assertEquals("iPhone 17", result.getName());
        assertEquals("Apple smartphone", result.getDescription());
        assertEquals(new BigDecimal("79999"), result.getPrice());
        assertEquals(true, result.getActive());

        ArgumentCaptor<Pageable> pageableCaptor =
                ArgumentCaptor.forClass(Pageable.class);

        verify(productRepository).searchProducts(
                eq("iphone"),
                eq(true),
                pageableCaptor.capture()
        );

        Pageable pageable = pageableCaptor.getValue();

        assertEquals(1, pageable.getPageNumber());
        assertEquals(5, pageable.getPageSize());
        assertEquals(
                Sort.Direction.DESC,
                pageable.getSort().getOrderFor("price").getDirection()
        );
    }

    @Test
    void searchProducts_shouldTrimSearchAndAllowMissingFilters() {

        when(productRepository.searchProducts(
                isNull(),
                isNull(),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of()));

        Page<ProductResponseDTO> response = productService.searchProducts(
                "   ",
                null,
                0,
                10,
                "id",
                "asc"
        );

        assertTrue(response.isEmpty());

        verify(productRepository).searchProducts(
                isNull(),
                isNull(),
                any(Pageable.class)
        );
    }

    @Test
    void searchProducts_shouldRejectNegativePage() {

        assertThrows(
                InvalidRequestException.class,
                () -> productService.searchProducts(
                        null, null, -1, 10, "id", "asc"
                )
        );

        verifyNoInteractions(productRepository);
    }

    @Test
    void searchProducts_shouldRejectInvalidPageSize() {

        assertThrows(
                InvalidRequestException.class,
                () -> productService.searchProducts(
                        null, null, 0, 101, "id", "asc"
                )
        );

        verifyNoInteractions(productRepository);
    }

    @Test
    void searchProducts_shouldRejectInvalidSortField() {

        assertThrows(
                InvalidRequestException.class,
                () -> productService.searchProducts(
                        null, null, 0, 10, "description", "asc"
                )
        );

        verifyNoInteractions(productRepository);
    }

    @Test
    void searchProducts_shouldRejectInvalidSortDirection() {

        assertThrows(
                InvalidRequestException.class,
                () -> productService.searchProducts(
                        null, null, 0, 10, "id", "sideways"
                )
        );

        verifyNoInteractions(productRepository);
    }

    private Product createProduct(
            Long id,
            String name,
            String description,
            String price,
            Boolean active
    ) {
        Product product = new Product();

        product.setId(id);
        product.setName(name);
        product.setDescription(description);
        product.setPrice(new BigDecimal(price));
        product.setActive(active);

        return product;
    }
}
