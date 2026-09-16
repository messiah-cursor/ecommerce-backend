package com.example.ecommerce.cart.repository;

import com.example.ecommerce.cart.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
}