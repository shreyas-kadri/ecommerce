package com.example.OrderService.Enum;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum PaymentMode {

    ONLINE,
    CASH_ON_DELIVERY;

    @JsonCreator
    public static PaymentMode fromString(String value) {
        return PaymentMode.valueOf(value.toUpperCase()); // Converts string to Enum
    }
}
