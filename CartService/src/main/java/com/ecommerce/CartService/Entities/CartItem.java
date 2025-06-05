package com.ecommerce.CartService.Entities;

import com.ecommerce.CartService.DTO.ProductDTO;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@SqlResultSetMapping(
        name = "ProductDTOResult",
        classes = @ConstructorResult(
                targetClass = ProductDTO.class,
                columns = {
                        @ColumnResult(name = "product_id", type = String.class),
                        @ColumnResult(name = "quantity", type = Integer.class)
                }
        )
)
@NamedNativeQuery(
        name = "CartItemRepository.getProductsInCartMapped",
        query = "SELECT product_id, quantity FROM cart_items WHERE cart_id = ?1",
        resultSetMapping = "ProductDTOResult"
)
@Data
@Table(name = "cart_items")
@AllArgsConstructor
@NoArgsConstructor
public class CartItem {

    @Id
    private String cartItemsId;

    private String productId;

    private int quantity;

    @ManyToOne
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

}
