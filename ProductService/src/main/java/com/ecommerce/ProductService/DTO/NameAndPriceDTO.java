package com.ecommerce.ProductService.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NameAndPriceDTO {

    private String name;

    private double price;
}
