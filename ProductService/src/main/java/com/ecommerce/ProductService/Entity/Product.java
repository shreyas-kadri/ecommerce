package com.ecommerce.ProductService.Entity;

import com.ecommerce.ProductService.Enum.Category;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Document(collection = "product_details")
public class Product {

    @Id
    private String productId;
    private String name;
    private double price;
    private String description;
    private Category category;
    private Map<String,String> details;
    private List<String> imageUrls;

}
