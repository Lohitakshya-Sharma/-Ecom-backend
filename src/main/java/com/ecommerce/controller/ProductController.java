package com.ecommerce.controller;

import com.ecommerce.dto.ProductRequestDTO;
import com.ecommerce.dto.ProductResponseDTO;
import com.ecommerce.dto.PurchaseRequestDTO;
import com.ecommerce.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(
        name = "Products",
        description = "APIs for managing products, searching inventory, and purchasing products"
)
@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @Operation(
            summary = "Create a new product",
            description = "Creates a product along with its initial inventory stock."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Product created successfully",
                    content = @Content(schema = @Schema(implementation = ProductResponseDTO.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid product data")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<ProductResponseDTO> createProduct(@Valid @RequestBody ProductRequestDTO dto) {
        ProductResponseDTO created = productService.createProduct(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(
            summary = "Get all products",
            description = "Returns a paginated list of all products in the catalog."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Products retrieved successfully"
    )
    @GetMapping
    public Page<ProductResponseDTO> getAllProducts(
            @Parameter(description = "Zero-based page index", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page (1-100)", example = "10")
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, Math.min(Math.max(1, size), 100));
        return productService.getAllProducts(pageable);
    }

    @Operation(
            summary = "Get a product by ID",
            description = "Returns the product details for the given product ID."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Product found successfully",
                    content = @Content(schema = @Schema(implementation = ProductResponseDTO.class))
            ),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    @GetMapping("/{id}")
    public ProductResponseDTO getProductById(
            @Parameter(description = "ID of the product to retrieve", example = "1")
            @PathVariable Long id) {
        return productService.getProductById(id);
    }

    @Operation(
            summary = "Update a product",
            description = "Updates the details of an existing product and its inventory stock."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Product updated successfully",
                    content = @Content(schema = @Schema(implementation = ProductResponseDTO.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid product data"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    @PutMapping("/{id}")
    public ProductResponseDTO updateProduct(
            @Parameter(description = "ID of the product to update", example = "1")
            @PathVariable Long id,
            @Valid @RequestBody ProductRequestDTO dto) {

        return productService.updateProduct(id, dto);
    }

    @Operation(
            summary = "Delete a product",
            description = "Deletes an existing product by its ID."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Product deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProduct(
            @Parameter(description = "ID of the product to delete", example = "1")
            @PathVariable Long id) {
        productService.deleteProduct(id);
    }

    @Operation(
            summary = "Purchase a product",
            description = "Purchases the requested quantity of a product and reduces its available inventory."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Purchase completed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid purchase quantity"),
            @ApiResponse(responseCode = "404", description = "Product not found"),
            @ApiResponse(responseCode = "409", description = "Insufficient stock")
    })
    @PostMapping("/{id}/purchase")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void purchaseProduct(
            @Parameter(description = "ID of the product to purchase", example = "1")
            @PathVariable Long id,
            @Valid @RequestBody PurchaseRequestDTO dto) {

        productService.purchaseProduct(id, dto.getQuantity());
    }

    @Operation(
            summary = "Search and filter products",
            description = "Search products by keyword (name and description), filter by active status, with pagination and sorting."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paginated products retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid query parameters (page, size, or sort field/direction)")
    })
    @GetMapping("/search")
    public Page<ProductResponseDTO> searchProducts(
            @Parameter(description = "Keyword to match against product name or description")
            @RequestParam(required = false) String search,
            @Parameter(description = "Filter by active (true/false) status")
            @RequestParam(required = false) Boolean active,
            @Parameter(description = "Zero-based page index", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page (1-100)", example = "10")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Field to sort by: id, name, price", example = "id")
            @RequestParam(defaultValue = "id") String sortBy,
            @Parameter(description = "Sort direction: asc or desc", example = "asc")
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
