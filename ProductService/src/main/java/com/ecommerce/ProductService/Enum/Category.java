package com.ecommerce.ProductService.Enum;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Category {
    ELECTRONICS,
    FASHION,
    HOME_APPLIANCES,
    BEAUTY,
    BOOKS;

    @JsonCreator
    public static Category fromString(String value) {
        return Category.valueOf(value.toUpperCase()); // Converts string to Enum
    }

}
