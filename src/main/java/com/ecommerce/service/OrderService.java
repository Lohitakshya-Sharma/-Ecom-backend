package com.ecommerce.service;

import com.ecommerce.dto.CreateOrderRequestDTO;
import com.ecommerce.dto.OrderItemRequestDTO;
import com.ecommerce.dto.OrderResponseDTO;
import com.ecommerce.entity.Inventory;
import com.ecommerce.entity.Order;
import com.ecommerce.entity.OrderItem;
import com.ecommerce.entity.OrderStatus;
import com.ecommerce.entity.Product;
import com.ecommerce.exception.InsufficientStockException;
import com.ecommerce.exception.InvalidOrderStateException;
import com.ecommerce.exception.InvalidRequestException;
import com.ecommerce.exception.OrderNotFoundException;
import com.ecommerce.exception.ProductNotFoundException;
import com.ecommerce.mapper.OrderMapper;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final OrderMapper orderMapper;

    public OrderService(OrderRepository orderRepository,
                        ProductRepository productRepository,
                        OrderMapper orderMapper) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.orderMapper = orderMapper;
    }

    @Retryable(
            retryFor = ObjectOptimisticLockingFailureException.class,
            maxAttempts = 5,
            backoff = @Backoff(delay = 50, multiplier = 2, random = true)
    )
    @Transactional
    public OrderResponseDTO createOrder(CreateOrderRequestDTO request) {

        Order order = new Order();
        order.setCustomerEmail(request.customerEmail());
        order.setCustomerName(request.customerName());
        order.setShippingAddress(request.shippingAddress());

        BigDecimal totalAmount = BigDecimal.ZERO;
        Set<Long> productIds = new HashSet<>();

        for (OrderItemRequestDTO itemRequest : request.items()) {

            Long productId = itemRequest.productId();

            if (!productIds.add(productId)) {
                throw new InvalidRequestException("Duplicate product ID: " + productId);
            }

            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new ProductNotFoundException(
                            "Product not found with ID: " + productId
                    ));

            if (!Boolean.TRUE.equals(product.getActive())) {
                throw new InvalidRequestException("Product is inactive: " + productId);
            }

            Inventory inventory = product.getInventory();

            if (inventory == null) {
                throw new IllegalStateException(
                        "Inventory not found for product ID: " + productId
                );
            }

            Integer requestedQuantity = itemRequest.quantity();

            if (inventory.getStock() < requestedQuantity) {
                throw new InsufficientStockException(
                        "Insufficient stock for product ID: " + productId
                );
            }

            BigDecimal unitPrice = product.getPrice();

            OrderItem orderItem = new OrderItem();
            orderItem.setProduct(product);
            orderItem.setProductName(product.getName());
            orderItem.setQuantity(requestedQuantity);
            orderItem.setUnitPrice(unitPrice);

            order.addOrderItem(orderItem);

            totalAmount = totalAmount.add(orderItem.getSubtotal());
            inventory.setStock(inventory.getStock() - requestedQuantity);
        }

        order.setTotalAmount(totalAmount);
        order.setStatus(OrderStatus.PENDING);

        Order savedOrder = orderRepository.save(order);

        return orderMapper.toOrderResponseDTO(savedOrder);
    }

    @Transactional(readOnly = true)
    public OrderResponseDTO getOrderById(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(
                        "Order not found with ID: " + orderId
                ));

        return orderMapper.toOrderResponseDTO(order);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponseDTO> getAllOrders(Pageable pageable) {
        Page<Long> idPage = orderRepository.findOrderIds(pageable);
        if (idPage.isEmpty()) {
            return Page.empty(pageable);
        }

        List<Long> ids = idPage.getContent();
        Map<Long, Order> ordersById = orderRepository.findWithItemsByIdIn(ids)
                .stream()
                .collect(Collectors.toMap(Order::getId, Function.identity()));

        List<OrderResponseDTO> content = ids.stream()
                .map(ordersById::get)
                .filter(Objects::nonNull)
                .map(orderMapper::toOrderResponseDTO)
                .toList();

        return new PageImpl<>(content, pageable, idPage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getAllOrders() {
        return orderRepository.findAll()
                .stream()
                .map(orderMapper::toOrderResponseDTO)
                .toList();
    }

    @Transactional
    public OrderResponseDTO confirmOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(
                        "Order not found with ID: " + orderId
                ));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new InvalidOrderStateException(
                    "Only pending orders can be confirmed"
            );
        }

        order.setStatus(OrderStatus.CONFIRMED);
        Order savedOrder = orderRepository.save(order);
        return orderMapper.toOrderResponseDTO(savedOrder);
    }

    @Transactional
    public OrderResponseDTO cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(
                        "Order not found with ID: " + orderId
                ));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new InvalidOrderStateException(
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

        return orderMapper.toOrderResponseDTO(savedOrder);
    }
}