package com.ecommerce.CartService.Repository;

import com.ecommerce.CartService.DTO.ProductDTO;
import com.ecommerce.CartService.Entities.CartItem;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;


public interface CartItemRepository extends JpaRepository<CartItem,String> {

    @Query(value = "SELECT CASE WHEN EXISTS (SELECT 1 FROM cart_items WHERE cart_id = ?1 AND product_id = ?2) THEN true ELSE false END", nativeQuery = true)
    boolean isProductInCart(String cartId, String productId);

    @Transactional
    @Modifying
    @Query(value="UPDATE cart_items SET quantity = ?3 WHERE cart_id = ?1 AND product_id = ?2",nativeQuery = true)
    void update(String cartId, String productId, int quantity);

    @Modifying
    @Transactional
    @Query(value="DELETE FROM cart_items WHERE cart_id = ?1 AND product_id = ?2",nativeQuery = true)
    void deleteProductFromCart(String cartId, String productId);

    @Query(name="CartItemRepository.getProductsInCartMapped",nativeQuery = true)
    List<ProductDTO> getProductsInCart(String cartId);

    @Transactional
    @Modifying
    @Query(value="DELETE FROM cart_items WHERE cart_id=?1",nativeQuery = true)
    void clearCart(String cartId);

}
