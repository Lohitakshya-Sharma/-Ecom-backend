package com.ecommerce.service;

import com.ecommerce.AbstractIntegrationTest;
import com.ecommerce.dto.CreateOrderRequestDTO;
import com.ecommerce.dto.OrderItemRequestDTO;
import com.ecommerce.dto.OrderResponseDTO;
import com.ecommerce.entity.Inventory;
import com.ecommerce.entity.Order;
import com.ecommerce.entity.OrderStatus;
import com.ecommerce.entity.Product;
import com.ecommerce.exception.InsufficientStockException;
import com.ecommerce.exception.InvalidOrderStateException;
import com.ecommerce.repository.InventoryRepository;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class InventoryConcurrencyTest extends AbstractIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private OrderRepository orderRepository;

    @BeforeEach
    void cleanDatabase() {
        orderRepository.deleteAll();
        productRepository.deleteAll();
    }

    @Test
    void shouldPreventOversellingUnderConcurrentOrderRequests() throws InterruptedException {
        Inventory inventory = new Inventory();
        inventory.setStock(5);

        Product product = new Product();
        product.setName("Limited Edition GPU");
        product.setDescription("High demand item");
        product.setPrice(new BigDecimal("500.00"));
        product.setActive(true);
        product.setInventory(inventory);

        Product savedProduct = productRepository.save(product);
        Long productId = savedProduct.getId();
        Long inventoryId = savedProduct.getInventory().getId();

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger expectedFailureCount = new AtomicInteger(0);
        AtomicInteger unexpectedErrorCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    CreateOrderRequestDTO request = new CreateOrderRequestDTO(
                            "buyer" + index + "@example.com",
                            "Buyer " + index,
                            "123 Street",
                            List.of(new OrderItemRequestDTO(productId, 1))
                    );
                    orderService.createOrder(request);
                    successCount.incrementAndGet();
                } catch (InsufficientStockException | ObjectOptimisticLockingFailureException ex) {
                    expectedFailureCount.incrementAndGet();
                } catch (Exception ex) {
                    unexpectedErrorCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        assertTrue(readyLatch.await(5, TimeUnit.SECONDS));
        startLatch.countDown();
        assertTrue(doneLatch.await(15, TimeUnit.SECONDS));
        executor.shutdown();

        assertEquals(0, unexpectedErrorCount.get(), "No unexpected exceptions should occur");
        assertTrue(successCount.get() >= 1 && successCount.get() <= 5,
                "Successful orders must be between 1 and initial stock (5), got: " + successCount.get());
        assertEquals(threadCount, successCount.get() + expectedFailureCount.get(),
                "Every request must either succeed or fail with a clean stock/concurrency exception");

        Inventory finalInventory = inventoryRepository.findById(inventoryId).orElseThrow();
        assertEquals(5 - successCount.get(), finalInventory.getStock(),
                "Remaining stock must equal initial stock minus committed orders");
        assertEquals(successCount.get(), orderRepository.count(),
                "Persisted order count must match successful order count");
    }

    @Test
    void shouldPreventRaceConditionBetweenConfirmAndCancel() throws InterruptedException {
        Inventory inventory = new Inventory();
        inventory.setStock(10);

        Product product = new Product();
        product.setName("Mechanical Keyboard");
        product.setDescription("Tactile switches");
        product.setPrice(new BigDecimal("150.00"));
        product.setActive(true);
        product.setInventory(inventory);

        Product savedProduct = productRepository.save(product);

        OrderResponseDTO createdOrder = orderService.createOrder(new CreateOrderRequestDTO(
                "customer@example.com",
                "Lohit",
                "Burla",
                List.of(new OrderItemRequestDTO(savedProduct.getId(), 2))
        ));
        Long orderId = createdOrder.id();
        Long inventoryId = savedProduct.getInventory().getId();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        executor.submit(() -> {
            readyLatch.countDown();
            try {
                startLatch.await();
                orderService.confirmOrder(orderId);
                successCount.incrementAndGet();
            } catch (ObjectOptimisticLockingFailureException | InvalidOrderStateException ex) {
                conflictCount.incrementAndGet();
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            } finally {
                doneLatch.countDown();
            }
        });

        executor.submit(() -> {
            readyLatch.countDown();
            try {
                startLatch.await();
                orderService.cancelOrder(orderId);
                successCount.incrementAndGet();
            } catch (ObjectOptimisticLockingFailureException | InvalidOrderStateException ex) {
                conflictCount.incrementAndGet();
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            } finally {
                doneLatch.countDown();
            }
        });

        assertTrue(readyLatch.await(5, TimeUnit.SECONDS));
        startLatch.countDown();
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS));
        executor.shutdown();

        assertEquals(1, successCount.get(), "Only one state transition should succeed");
        assertEquals(1, conflictCount.get(), "The losing transition must fail with a conflict");

        Order finalOrder = orderRepository.findById(orderId).orElseThrow();
        Inventory finalInventory = inventoryRepository.findById(inventoryId).orElseThrow();

        if (finalOrder.getStatus() == OrderStatus.CONFIRMED) {
            assertEquals(8, finalInventory.getStock(), "Confirmed order must keep stock deducted");
        } else {
            assertEquals(OrderStatus.CANCELLED, finalOrder.getStatus());
            assertEquals(10, finalInventory.getStock(), "Cancelled order must restore stock");
        }
    }
}