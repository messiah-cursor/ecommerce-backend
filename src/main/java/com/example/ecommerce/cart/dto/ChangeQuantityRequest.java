package com.example.ecommerce.cart.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangeQuantityRequest {
    @NotNull(message = "Quantity can not be empty.")
    private Long change;
}
