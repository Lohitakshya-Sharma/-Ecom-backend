package com.ecommerce.controller;

import com.ecommerce.dto.CreateOrderRequestDTO;
import com.ecommerce.dto.OrderResponseDTO;
import com.ecommerce.service.OrderIdempotencyService;
import com.ecommerce.service.OrderService;
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
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(
        name = "Orders",
        description = "APIs for creating, tracking, listing, and cancelling customer orders"
)
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;
    private final OrderIdempotencyService orderIdempotencyService;

    public OrderController(OrderService orderService,
                           OrderIdempotencyService orderIdempotencyService) {
        this.orderService = orderService;
        this.orderIdempotencyService = orderIdempotencyService;
    }

    @Operation(
            summary = "Create a new order",
            description = "Validates items, checks stock availability, deducts inventory, and places an order in PENDING status. Supports an optional Idempotency-Key header for safe retries."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Order created successfully",
                    content = @Content(schema = @Schema(implementation = OrderResponseDTO.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid request payload, inactive product, or invalid Idempotency-Key"),
            @ApiResponse(responseCode = "404", description = "One or more products not found"),
            @ApiResponse(responseCode = "409", description = "Insufficient inventory stock, idempotency key payload conflict, or concurrent update conflict")
    })
    @PostMapping
    public ResponseEntity<OrderResponseDTO> createOrder(
            @Parameter(description = "Optional client-generated idempotency key (max 128 characters) to safely retry order creation", example = "order-req-12345")
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreateOrderRequestDTO request
    ) {
        OrderResponseDTO response = orderIdempotencyService.createOrder(idempotencyKey, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Get order by ID",
            description = "Retrieves the order details, line items, and current status for the given order ID."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Order found successfully",
                    content = @Content(schema = @Schema(implementation = OrderResponseDTO.class))
            ),
            @ApiResponse(responseCode = "404", description = "Order not found")
    })
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponseDTO> getOrderById(
            @Parameter(description = "ID of the order to retrieve", example = "1")
            @PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.getOrderById(orderId));
    }

    @Operation(
            summary = "Get all orders",
            description = "Returns a paginated list of all customer orders in the system."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Orders retrieved successfully"
            )
    })
    @GetMapping
    public ResponseEntity<Page<OrderResponseDTO>> getAllOrders(
            @Parameter(description = "Zero-based page index", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page (1-100)", example = "10")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort direction: asc or desc", example = "desc")
            @RequestParam(defaultValue = "desc") String direction
    ) {
        Sort.Direction sortDir = Sort.Direction.fromString(direction);
        Pageable pageable = PageRequest.of(page, Math.min(Math.max(1, size), 100), Sort.by(sortDir, "id"));
        return ResponseEntity.ok(orderService.getAllOrders(pageable));
    }

    @Operation(
            summary = "Cancel an order",
            description = "Cancels a pending order and restores reserved stock back to product inventory."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Order cancelled and inventory restored successfully",
                    content = @Content(schema = @Schema(implementation = OrderResponseDTO.class))
            ),
            @ApiResponse(responseCode = "404", description = "Order not found"),
            @ApiResponse(responseCode = "409", description = "Order is not in PENDING status and cannot be cancelled")
    })
    @PatchMapping("/{orderId}/cancel")
    public ResponseEntity<OrderResponseDTO> cancelOrder(
            @Parameter(description = "ID of the order to cancel", example = "1")
            @PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.cancelOrder(orderId));
    }

    @Operation(
            summary = "Confirm an order",
            description = "Transitions a pending order to CONFIRMED status."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Order confirmed successfully",
                    content = @Content(schema = @Schema(implementation = OrderResponseDTO.class))
            ),
            @ApiResponse(responseCode = "404", description = "Order not found"),
            @ApiResponse(responseCode = "409", description = "Order is not in PENDING status and cannot be confirmed")
    })
    @PatchMapping("/{orderId}/confirm")
    public ResponseEntity<OrderResponseDTO> confirmOrder(
            @Parameter(description = "ID of the order to confirm", example = "1")
            @PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.confirmOrder(orderId));
    }
}