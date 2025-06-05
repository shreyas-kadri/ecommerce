package com.ecommerce.CartService.Repository;

import com.ecommerce.CartService.Entities.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart,String>{

    @Query(value="SELECT cart_id FROM cart WHERE user_id = ?1",nativeQuery = true)
    Optional<String> findCartId(String userId);

    @Query(value = "SELECT COUNT(*) > 0 FROM cart WHERE user_id = ?1", nativeQuery = true)
    boolean doesCartExist(String userId);

}
