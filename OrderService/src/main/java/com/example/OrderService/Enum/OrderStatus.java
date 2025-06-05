package com.example.OrderService.Enum;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum OrderStatus {

    PLACED,
    TRANSIT,
    DELIVERED;

    @JsonCreator
    public static OrderStatus fromString(String value) {
        return OrderStatus.valueOf(value.toUpperCase()); // Converts string to Enum
    }
}
