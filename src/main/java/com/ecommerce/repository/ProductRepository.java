package com.ecommerce.repository;

import com.ecommerce.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query("""
            SELECT p FROM Product p
            WHERE (
                CAST(:search AS string) IS NULL
                OR LOWER(p.name) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
                OR LOWER(p.description) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
            )
            AND (:active IS NULL OR p.active = :active)
            """)
    Page<Product> searchProducts(
            @Param("search") String search,
            @Param("active") Boolean active,
            Pageable pageable
    );
}