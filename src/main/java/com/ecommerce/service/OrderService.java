package com.ecommerce.service;

import com.ecommerce.dto.OrderItemResponseDTO;
import com.ecommerce.dto.OrderResponseDTO;
import com.ecommerce.entity.Order;
import com.ecommerce.exception.OrderNotFoundException;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.dto.CreateOrderRequestDTO;
import com.ecommerce.dto.OrderItemRequestDTO;
import com.ecommerce.entity.Inventory;
import com.ecommerce.entity.OrderItem;
import com.ecommerce.entity.OrderStatus;
import com.ecommerce.entity.Product;
import com.ecommerce.exception.InsufficientStockException;
import com.ecommerce.exception.ProductNotFoundException;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    public OrderService(OrderRepository orderRepository,
                        ProductRepository productRepository
    ) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public OrderResponseDTO createOrder(CreateOrderRequestDTO request) {

        Order order = new Order();
        BigDecimal totalAmount = BigDecimal.ZERO;

        Set<Long> productIds = new HashSet<>();

        for (OrderItemRequestDTO itemRequest : request.getItems()) {

            Long productId = itemRequest.getProductId();

            if (!productIds.add(productId)) {
                throw new IllegalArgumentException(
                        "Duplicate product ID: " + productId
                );
            }

            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new ProductNotFoundException(
                            "Product not found with ID: " + productId
                    ));

            if (!Boolean.TRUE.equals(product.getActive())) {
                throw new IllegalArgumentException(
                        "Product is inactive: " + productId
                );
            }

            Inventory inventory = product.getInventory();

            if (inventory == null) {
                throw new IllegalStateException(
                        "Inventory not found for product ID: " + productId
                );
            }

            Integer requestedQuantity = itemRequest.getQuantity();

            if (inventory.getStock() < requestedQuantity) {
                throw new InsufficientStockException(
                        "Insufficient stock for product ID: " + productId
                );
            }

            BigDecimal unitPrice = product.getPrice();

            OrderItem orderItem = new OrderItem();
            orderItem.setProduct(product);
            orderItem.setQuantity(requestedQuantity);
            orderItem.setUnitPrice(unitPrice);

            order.addOrderItem(orderItem);

            totalAmount = totalAmount.add(orderItem.getSubtotal());
            inventory.setStock(inventory.getStock() - requestedQuantity);
        }

        order.setTotalAmount(totalAmount);
        order.setStatus(OrderStatus.PENDING);

        Order savedOrder = orderRepository.save(order);

        return mapOrderToResponseDTO(savedOrder);
    }

    @Transactional(readOnly = true)
    public OrderResponseDTO getOrderById(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(
                        "Order not found with ID: " + orderId
                ));

        return mapOrderToResponseDTO(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getAllOrders() {
        return orderRepository.findAll()
                .stream()
                .map(this::mapOrderToResponseDTO)
                .toList();
    }


    @Transactional
    public OrderResponseDTO cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(
                        "Order not found with ID: " + orderId
                ));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException(
                    "Only pending orders can be cancelled"
            );
        }

        for (OrderItem item : order.getOrderItems()) {
            Inventory inventory = item.getProduct().getInventory();

            if (inventory == null) {
                throw new IllegalStateException(
                        "Inventory not found for product ID: "
                                + item.getProduct().getId()
                );
            }

            inventory.setStock(
                    inventory.getStock() + item.getQuantity()
            );
        }

        order.setStatus(OrderStatus.CANCELLED);

        Order savedOrder = orderRepository.save(order);

        return mapOrderToResponseDTO(savedOrder);
    }


    private OrderResponseDTO mapOrderToResponseDTO(Order order) {
        OrderResponseDTO response = new OrderResponseDTO();

        response.setId(order.getId());
        response.setOrderDate(order.getOrderDate());
        response.setStatus(order.getStatus());
        response.setTotalAmount(order.getTotalAmount());

        List<OrderItemResponseDTO> itemResponses = order.getOrderItems()
                .stream()
                .map(this::mapOrderItemToResponseDTO)
                .toList();

        response.setOrderItems(itemResponses);

        return response;
    }

    private OrderItemResponseDTO mapOrderItemToResponseDTO(OrderItem item) {
        OrderItemResponseDTO response = new OrderItemResponseDTO();

        response.setId(item.getId());
        response.setProductId(item.getProduct().getId());
        response.setProductName(item.getProduct().getName());
        response.setQuantity(item.getQuantity());
        response.setUnitPrice(item.getUnitPrice());
        response.setSubtotal(item.getSubtotal());

        return response;
    }

}