package com.ecommerce.InventoryService.Repository;

import com.ecommerce.InventoryService.Entity.Product;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import javax.swing.text.html.Option;
import java.util.List;
import java.util.Optional;


public interface InventoryRepository extends JpaRepository<Product, String> {

    @Query(value="SELECT * FROM products WHERE seller_id = ?1",nativeQuery = true)
    List<Product> findAllProductsBySellerId(String sellerId);

    @Query(value="SELECT * FROM products WHERE product_id = ?1 AND seller_id = ?2",nativeQuery = true)
    Optional<Product> findProductBySellerId(String productId,String sellerId);

    @Query(value = "SELECT EXISTS (SELECT 1 FROM products WHERE product_id = ?1 AND stock >= ?2)", nativeQuery = true)
    boolean isInStock(String productId, int quantity);

    @Query(value = "SELECT * FROM products WHERE product_id IN (:productIds)", nativeQuery = true)
    List<Product> getCartProducts(@Param("productIds") List<String> productIds);

    @Query(value = "SELECT product_id FROM products WHERE seller_id = ?1",nativeQuery = true)
    List<String> getProductIds(String sellerId);

    @Transactional
    @Modifying
    @Query(value = "UPDATE products SET stock = stock - ?2 WHERE product_id = ?1",nativeQuery = true)
    void reduceStock(String productId, int quantity);
}
