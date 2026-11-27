package com.ecommerce.service;

import com.ecommerce.dto.CreateOrderRequestDTO;
import com.ecommerce.dto.OrderItemRequestDTO;
import com.ecommerce.dto.OrderResponseDTO;
import com.ecommerce.entity.IdempotencyRecord;
import com.ecommerce.exception.IdempotencyConflictException;
import com.ecommerce.exception.InvalidRequestException;
import com.ecommerce.repository.IdempotencyRecordRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class OrderIdempotencyService {

    private static final int MAX_KEY_LENGTH = 128;

    private final OrderService orderService;
    private final IdempotencyRecordRepository idempotencyRecordRepository;

    public OrderIdempotencyService(
            OrderService orderService,
            IdempotencyRecordRepository idempotencyRecordRepository
    ) {
        this.orderService = orderService;
        this.idempotencyRecordRepository = idempotencyRecordRepository;
    }

    public OrderResponseDTO createOrder(String rawIdempotencyKey, CreateOrderRequestDTO request) {
        if (rawIdempotencyKey == null || rawIdempotencyKey.isBlank()) {
            return orderService.createOrder(request);
        }

        String idempotencyKey = rawIdempotencyKey.trim();
        if (idempotencyKey.length() > MAX_KEY_LENGTH) {
            throw new InvalidRequestException("Idempotency-Key header must not exceed 128 characters");
        }

        String requestHash = computeRequestHash(request);

        Optional<IdempotencyRecord> existing = idempotencyRecordRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return resolveExistingRecord(existing.get(), requestHash);
        }

        try {
            return orderService.createOrderWithIdempotency(idempotencyKey, requestHash, request);
        } catch (DataIntegrityViolationException ex) {
            IdempotencyRecord concurrentRecord = idempotencyRecordRepository
                    .findByIdempotencyKey(idempotencyKey)
                    .orElseThrow(() -> ex);
            return resolveExistingRecord(concurrentRecord, requestHash);
        }
    }

    private OrderResponseDTO resolveExistingRecord(IdempotencyRecord record, String incomingHash) {
        if (!record.getRequestHash().equals(incomingHash)) {
            throw new IdempotencyConflictException(
                    "Idempotency-Key '" + record.getIdempotencyKey() + "' was already used with a different request payload"
            );
        }
        return orderService.getOrderById(record.getOrderId());
    }

    private String computeRequestHash(CreateOrderRequestDTO request) {
        String itemsCanonical = request.items().stream()
                .map(this::formatItem)
                .collect(Collectors.joining(","));

        String canonicalPayload = String.join("|",
                request.customerEmail().trim().toLowerCase(),
                request.customerName().trim(),
                request.shippingAddress().trim(),
                itemsCanonical
        );

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(canonicalPayload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", e);
        }
    }

    private String formatItem(OrderItemRequestDTO item) {
        return item.productId() + ":" + item.quantity();
    }
}