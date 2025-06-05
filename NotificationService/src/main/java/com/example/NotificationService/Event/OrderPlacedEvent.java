package com.example.NotificationService.Event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderPlacedEvent {
    private String orderId;
    private String userId;
    private String userName;
    private String phoneNumber;
    private String email;
    private double amount;
    private String shippingAddress;
}

