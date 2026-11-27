package com.ecommerce.repository;

import com.ecommerce.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("SELECT o.id FROM Order o")
    Page<Long> findOrderIds(Pageable pageable);

    @EntityGraph(attributePaths = {"orderItems", "orderItems.product"})
    List<Order> findWithItemsByIdIn(@Param("ids") List<Long> ids);

    @Override
    @EntityGraph(attributePaths = {"orderItems", "orderItems.product"})
    List<Order> findAll();

    @Override
    @EntityGraph(attributePaths = {"orderItems", "orderItems.product"})
    Optional<Order> findById(Long id);
}