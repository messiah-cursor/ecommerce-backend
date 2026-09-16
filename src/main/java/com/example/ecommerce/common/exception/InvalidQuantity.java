package com.example.ecommerce.common.exception;

public class InvalidQuantity extends RuntimeException {
    public InvalidQuantity(String message) {
        super(message);
    }
}
