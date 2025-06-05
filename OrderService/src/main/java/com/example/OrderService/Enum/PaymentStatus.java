package com.example.OrderService.Enum;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum PaymentStatus {

    PAID,
    PENDING;

    @JsonCreator
    public static PaymentStatus fromString(String value) {
        return PaymentStatus.valueOf(value.toUpperCase()); // Converts string to Enum
    }
}
