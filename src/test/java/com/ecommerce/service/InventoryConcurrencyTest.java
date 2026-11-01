package com.ecommerce.service;

import com.ecommerce.entity.Inventory;
import com.ecommerce.repository.InventoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
public class InventoryConcurrencyTest {

    @Autowired
    private InventoryRepository inventoryRepository;

    @Test
    void shouldDetectOptimisticLockingConflict() {

        // Create inventory with stock = 10
        Inventory inventory = new Inventory();
        inventory.setStock(10);

        Inventory saved = inventoryRepository.save(inventory);

        Long id = saved.getId();

        // Two different transactions read the same inventory
        Inventory transaction1 = inventoryRepository.findById(id).orElseThrow();
        Inventory transaction2 = inventoryRepository.findById(id).orElseThrow();

        // Both initially see the same version
        System.out.println("Transaction 1 version: " + transaction1.getVersion());
        System.out.println("Transaction 2 version: " + transaction2.getVersion());

        // Transaction 1 updates stock
        transaction1.setStock(9);
        inventoryRepository.save(transaction1);

        // Transaction 1 has now moved the version forward
        System.out.println("Transaction 1 updated stock.");

        // Transaction 2 is still holding the old version
        transaction2.setStock(8);

        // Hibernate should reject this stale update
        assertThrows(
                ObjectOptimisticLockingFailureException.class,
                () -> inventoryRepository.saveAndFlush(transaction2)
        );

        // Cleanup
        inventoryRepository.deleteById(id);
    }
}