package com.example.ecommerce.cart.controller;

import com.example.ecommerce.cart.dto.*;
import com.example.ecommerce.cart.service.CartService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class CartController {
    private final CartService service;

    public CartController(CartService service) {
        this.service = service;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<CartResponseDTO> getCart(@PathVariable Long userId){
        CartResponseDTO response = service.getCart(userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/users/{userId}/cart/items")
    public ResponseEntity<List<CartItemDTO>> addToCart(@PathVariable Long userId, @Valid @RequestBody AddToCartRequest request){
        List<CartItemDTO> items = service.addToCart(userId, request.getProductId(), request.getQuantity());
        return ResponseEntity.status(HttpStatus.CREATED).body(items);
    }

    @DeleteMapping("/delete/{userId}/{cartItemId}")
    public ResponseEntity<Void> removeFromCart(@PathVariable Long userId, @PathVariable Long cartItemId){
        service.removeFromCart(userId, cartItemId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/users/{userId}/cart/items/{cartItemId}/quantity")
    public ResponseEntity<List<CartItemDTO>> changeCartQuantity(@PathVariable Long userId, @PathVariable Long cartItemId, @Valid @RequestBody ChangeQuantityRequest request){
        List<CartItemDTO> updatedCart = service.changeQuantity(userId, cartItemId, request.getChange());
        return ResponseEntity.ok(updatedCart);
    }

    @PutMapping("/update/cart/{userId}")
    public ResponseEntity<List<CartItemDTO>> updateQuantity(@PathVariable Long userId, @Valid @RequestBody AddToCartRequest request){
        List<CartItemDTO> updatedCart = service.updateQuantity(userId, request.getProductId(), request.getQuantity());
       return ResponseEntity.ok(updatedCart);
    }

    @DeleteMapping("/clear/{userId}")
    public ResponseEntity<Void> clearCart(@PathVariable Long userId){
        service.clearCart(userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/order/{userId}")
    public ResponseEntity<OrderResponseDTO> order(@PathVariable Long userId){
        OrderResponseDTO order = service.order(userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }
}
