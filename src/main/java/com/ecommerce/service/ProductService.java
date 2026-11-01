package com.ecommerce.service;

import com.ecommerce.dto.ProductRequestDTO;
import com.ecommerce.dto.ProductResponseDTO;
import com.ecommerce.entity.Inventory;
import com.ecommerce.entity.Product;
import com.ecommerce.exception.InsufficientStockException;
import com.ecommerce.exception.ProductNotFoundException;
import com.ecommerce.repository.InventoryRepository;
import com.ecommerce.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

@Service
public class ProductService {


    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;

    public ProductService(ProductRepository productRepository, InventoryRepository inventoryRepository) {

        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
    }

    @Transactional
    public ProductResponseDTO createProduct(ProductRequestDTO dto) {

        Product product = new Product();

        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        product.setActive(dto.getActive());

        Product savedProduct = productRepository.save(product);

        Inventory inventory = new Inventory();
        inventory.setStock(dto.getStock());

        Inventory savedInventory = inventoryRepository.save(inventory);

        savedProduct.setInventory(savedInventory);
        productRepository.save(savedProduct);

        ProductResponseDTO response = new ProductResponseDTO();

        response.setId(savedProduct.getId());
        response.setName(savedProduct.getName());
        response.setDescription(savedProduct.getDescription());
        response.setPrice(savedProduct.getPrice());
        response.setActive(savedProduct.getActive());
        response.setStock(savedInventory.getStock());

        return response;
    }

    @Transactional
    public void purchaseProduct(Long productId, int quantity) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() ->
                        new ProductNotFoundException(
                                "Product not found with id: " + productId));

        Inventory inventory = product.getInventory();

        if (inventory.getStock() < quantity) {
            throw new InsufficientStockException(
                    "Insufficient stock. Available: " + inventory.getStock());
        }

        inventory.setStock(inventory.getStock() - quantity);

        inventoryRepository.save(inventory);
    }

    public List<ProductResponseDTO> getAllProducts() {

        List<Product> products = productRepository.findAll();

        return products.stream()
                .map(product -> {
                    ProductResponseDTO response = new ProductResponseDTO();

                    response.setId(product.getId());
                    response.setName(product.getName());
                    response.setDescription(product.getDescription());
                    response.setPrice(product.getPrice());
                    response.setActive(product.getActive());

                    response.setStock(
                            product.getInventory() != null
                                    ? product.getInventory().getStock()
                                    : null
                    );

                    return response;
                })
                .toList();
    }


    public ProductResponseDTO getProductById(Long id) {

        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new ProductNotFoundException(
                                "Product not found with id: " + id
                        ));

        ProductResponseDTO response = new ProductResponseDTO();

        response.setId(product.getId());
        response.setName(product.getName());
        response.setDescription(product.getDescription());
        response.setPrice(product.getPrice());
        response.setActive(product.getActive());

        response.setStock(
                product.getInventory() != null
                        ? product.getInventory().getStock()
                        : null
        );

        return response;
    }

    public ProductResponseDTO updateProduct(Long id, ProductRequestDTO dto) {

        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new ProductNotFoundException(
                                "Product not found with id: " + id
                        ));

        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        product.setActive(dto.getActive());

        Product updatedProduct = productRepository.save(product);

        ProductResponseDTO response = new ProductResponseDTO();

        response.setId(updatedProduct.getId());
        response.setName(updatedProduct.getName());
        response.setDescription(updatedProduct.getDescription());
        response.setPrice(updatedProduct.getPrice());
        response.setActive(updatedProduct.getActive());

        response.setStock(
                updatedProduct.getInventory() != null
                        ? updatedProduct.getInventory().getStock()
                        : null
        );

        return response;
    }

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
            throw new IllegalArgumentException("Page must be zero or greater");
        }

        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("Page size must be between 1 and 100");
        }

        if (!List.of("id", "name", "price").contains(sortBy)) {
            throw new IllegalArgumentException(
                    "sortBy must be one of: id, name, price"
            );
        }

        Sort.Direction sortDirection;

        try {
            sortDirection = Sort.Direction.fromString(direction);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
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
                .map(this::toProductResponseDTO);
    }

    private ProductResponseDTO toProductResponseDTO(Product product) {
        ProductResponseDTO response = new ProductResponseDTO();

        response.setId(product.getId());
        response.setName(product.getName());
        response.setDescription(product.getDescription());
        response.setPrice(product.getPrice());
        response.setActive(product.getActive());

        response.setStock(
                product.getInventory() != null
                        ? product.getInventory().getStock()
                        : null
        );

        return response;
    }
}
