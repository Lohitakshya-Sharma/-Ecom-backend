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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final ProductMapper productMapper;

    public ProductService(ProductRepository productRepository,
                          InventoryRepository inventoryRepository,
                          ProductMapper productMapper) {
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
        this.productMapper = productMapper;
    }

    @Transactional
    public ProductResponseDTO createProduct(ProductRequestDTO dto) {
        Product product = productMapper.toProduct(dto);

        Inventory inventory = new Inventory();
        inventory.setStock(dto.stock());
        Inventory savedInventory = inventoryRepository.save(inventory);

        product.setInventory(savedInventory);
        Product savedProduct = productRepository.save(product);

        return productMapper.toProductResponseDTO(savedProduct);
    }

    @Retryable(
            retryFor = ObjectOptimisticLockingFailureException.class,
            maxAttempts = 5,
            backoff = @Backoff(delay = 50, multiplier = 2, random = true)
    )
    @Transactional
    public void purchaseProduct(Long productId, int quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() ->
                        new ProductNotFoundException(
                                "Product not found with id: " + productId));

        if (!Boolean.TRUE.equals(product.getActive())) {
            throw new InvalidRequestException("Product is inactive: " + productId);
        }

        Inventory inventory = product.getInventory();
        if (inventory == null) {
            throw new IllegalStateException("Inventory not found for product id: " + productId);
        }

        if (inventory.getStock() < quantity) {
            throw new InsufficientStockException(
                    "Insufficient stock. Available: " + inventory.getStock());
        }

        inventory.setStock(inventory.getStock() - quantity);
        inventoryRepository.save(inventory);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponseDTO> getAllProducts(Pageable pageable) {
        return productRepository.findAll(pageable)
                .map(productMapper::toProductResponseDTO);
    }

    @Transactional(readOnly = true)
    public List<ProductResponseDTO> getAllProducts() {
        return productRepository.findAll()
                .stream()
                .map(productMapper::toProductResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductResponseDTO getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new ProductNotFoundException(
                                "Product not found with id: " + id
                        ));

        return productMapper.toProductResponseDTO(product);
    }

    @Transactional
    public ProductResponseDTO updateProduct(Long id, ProductUpdateRequestDTO dto) {
        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new ProductNotFoundException(
                                "Product not found with id: " + id
                        ));

        product.setName(dto.name());
        product.setDescription(dto.description());
        product.setPrice(dto.price());
        product.setActive(dto.active());

        Product updatedProduct = productRepository.save(product);
        return productMapper.toProductResponseDTO(updatedProduct);
    }

    @Retryable(
            retryFor = ObjectOptimisticLockingFailureException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 50, multiplier = 2, random = true)
    )
    @Transactional
    public ProductResponseDTO adjustStock(Long id, int delta) {
        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new ProductNotFoundException(
                                "Product not found with id: " + id
                        ));

        Inventory inventory = product.getInventory();
        if (inventory == null) {
            throw new IllegalStateException("Inventory not found for product id: " + id);
        }

        int updatedStock = inventory.getStock() + delta;
        if (updatedStock < 0) {
            throw new InsufficientStockException(
                    "Insufficient stock. Current stock: " + inventory.getStock()
            );
        }

        inventory.setStock(updatedStock);
        inventoryRepository.save(inventory);

        return productMapper.toProductResponseDTO(product);
    }

    @Transactional
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new ProductNotFoundException("Product not found with id: " + id));

        productRepository.delete(product);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponseDTO> searchProducts(
            String search,
            Boolean active,
            int page,
            int size,
            String sortBy,
            String direction
    ) {
        if (page < 0) {
            throw new InvalidRequestException("Page must be zero or greater");
        }

        if (size < 1 || size > 100) {
            throw new InvalidRequestException("Page size must be between 1 and 100");
        }

        if (!List.of("id", "name", "price").contains(sortBy)) {
            throw new InvalidRequestException(
                    "sortBy must be one of: id, name, price"
            );
        }

        Sort.Direction sortDirection;
        try {
            sortDirection = Sort.Direction.fromString(direction);
        } catch (IllegalArgumentException exception) {
            throw new InvalidRequestException(
                    "direction must be 'asc' or 'desc'"
            );
        }

        String normalizedSearch =
                search == null || search.isBlank() ? null : search.trim();

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(sortDirection, sortBy)
        );

        return productRepository.searchProducts(
                        normalizedSearch,
                        active,
                        pageable
                )
                .map(productMapper::toProductResponseDTO);
    }
}