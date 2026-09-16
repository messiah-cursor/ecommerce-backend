package com.example.ecommerce.cart.repository;

import com.example.ecommerce.cart.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}