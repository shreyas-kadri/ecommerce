package com.ecommerce.ProductService.DTO;

import com.ecommerce.ProductService.Enum.Category;
import com.fasterxml.jackson.annotation.JsonCreator;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductDTO implements Serializable {

    @NotBlank(message = "Product id is required")
    private String productId;

    @NotBlank(message = "Description is required")
    private String description;

    private Category category;

    private Map<String,String> details;

}
