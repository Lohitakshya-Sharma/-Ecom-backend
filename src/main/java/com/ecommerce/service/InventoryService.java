package com.ecommerce.service;

import com.ecommerce.entity.Inventory;
import com.ecommerce.repository.InventoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    public InventoryService(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    @Transactional
    public void updateStock(Long id, int newStock) {
        Inventory inventory = inventoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Inventory not found with id: " + id));

        inventory.setStock(newStock);
        inventoryRepository.save(inventory);
    }
}
