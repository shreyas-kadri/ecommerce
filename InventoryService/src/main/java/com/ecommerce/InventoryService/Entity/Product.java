package com.ecommerce.InventoryService.Entity;

import com.ecommerce.InventoryService.DTO.ProductDTO;
import jakarta.persistence.*;
import lombok.*;


@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name="products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String productId;
    private String sellerId;
    private String name;
    private double price;
    private int stock;

    // Method to convert Product entity to ProductDTO
    public ProductDTO toDTO() {
        return new ProductDTO(name,price,stock);
    }
}