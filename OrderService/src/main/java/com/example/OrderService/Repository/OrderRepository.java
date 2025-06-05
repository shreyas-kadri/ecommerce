package com.example.OrderService.Repository;

import com.example.OrderService.Entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order,String> {

    @Query(value = "SELECT * FROM orders WHERE user_id = ?1",nativeQuery = true)
    List<Order> getOrderHistory(String userId);
}
