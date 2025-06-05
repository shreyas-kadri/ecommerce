package com.example.OrderService.DTO;

import com.example.OrderService.Enum.PaymentMode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderDTO {

    private String shippingAddress;

    private PaymentMode paymentMode;

}
