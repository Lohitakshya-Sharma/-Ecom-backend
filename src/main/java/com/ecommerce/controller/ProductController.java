package com.ecommerce.controller;

import com.ecommerce.dto.ProductRequestDTO;
import com.ecommerce.dto.ProductResponseDTO;
import com.ecommerce.dto.PurchaseRequestDTO;
import com.ecommerce.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Tag(
        name = "Products",
        description = "APIs for managing products and purchasing products"
)
@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @Operation(
            summary = "Create a new product",
            description = "Creates a product along with its inventory."
    )
    @ApiResponse(
            responseCode = "201",
            description = "Product created successfully"
    )
    @ApiResponse(
            responseCode = "400",
            description = "Invalid product data"
    )
    @PostMapping
    public ProductResponseDTO createProduct(@Valid @RequestBody ProductRequestDTO dto) {
        return productService.createProduct(dto);
    }

    @Operation(
            summary = "Get all products",
            description = "Returns a list of all products."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Products retrieved successfully"
    )
    @GetMapping
    public List<ProductResponseDTO> getAllProducts() {
        return productService.getAllProducts();
    }

    @Operation(
            summary = "Get a product by ID",
            description = "Returns the product details for the given product ID."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Product found successfully"
    )
    @ApiResponse(
            responseCode = "404",
            description = "Product not found"
    )
    @GetMapping("/{id}")
    public ProductResponseDTO getProductById(@PathVariable Long id) {
        return productService.getProductById(id);
    }

    @Operation(
            summary = "Update a product",
            description = "Updates the details of an existing product."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Product updated successfully"
    )
    @ApiResponse(
            responseCode = "400",
            description = "Invalid product data"
    )
    @ApiResponse(
            responseCode = "404",
            description = "Product not found"
    )
    @PutMapping("/{id}")
    public ProductResponseDTO updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequestDTO dto) {

        return productService.updateProduct(id, dto);
    }

    @Operation(
            summary = "Delete a product",
            description = "Deletes an existing product by its ID."
    )
    @ApiResponse(
            responseCode = "204",
            description = "Product deleted successfully"
    )
    @ApiResponse(
            responseCode = "404",
            description = "Product not found"
    )
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
    }

    @Operation(
            summary = "Purchase a product",
            description = "Purchases the requested quantity of a product and reduces its available inventory."
    )
    @ApiResponse(
            responseCode = "204",
            description = "Purchase completed successfully"
    )
    @ApiResponse(
            responseCode = "400",
            description = "Invalid purchase quantity"
    )
    @ApiResponse(
            responseCode = "404",
            description = "Product not found"
    )
    @ApiResponse(
            responseCode = "409",
            description = "Insufficient stock"
    )
    @PostMapping("/{id}/purchase")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void purchaseProduct(
            @PathVariable Long id,
            @Valid @RequestBody PurchaseRequestDTO dto) {

        productService.purchaseProduct(id, dto.getQuantity());
    }

    @GetMapping("/search")
    public Page<ProductResponseDTO> searchProducts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction
    ) {
        return productService.searchProducts(
                search,
                active,
                page,
                size,
                sortBy,
                direction
        );
    }
}
