package com.example.OrderService.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@Table(name = "order_item")
@AllArgsConstructor
@NoArgsConstructor
public class OrderItem {

    @Id
    private String orderItemId;

    private String productId;

    private String name;

    private double price;

    private int quantity;

    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    @JsonIgnore
    private Order order;

}
