package com.example.OrderService.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CartSummaryDTO {

    private String cartId;

    private List<CartProductDTO> products;

    private double totalBill;

}
