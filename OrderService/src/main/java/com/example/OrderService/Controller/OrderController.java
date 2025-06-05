package com.example.OrderService.Controller;

import com.example.OrderService.DTO.OrderDTO;
import com.example.OrderService.Entity.Order;
import com.example.OrderService.Service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/order")
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/placeOrder")
    public ResponseEntity<String> placeOrder(@RequestHeader("Authorization") String authorizationHeader, @RequestBody OrderDTO orderDTO)
    {
        String accessToken = authorizationHeader.substring(7);
        return orderService.placeOrder(accessToken,orderDTO);
    }

    @GetMapping("/getOrderHistory")
    public List<Order> getOrderHistory(@RequestHeader("Authorization") String authorizationHeader)
    {
        String accessToken = authorizationHeader.substring(7);
        return orderService.getOrderHistory(accessToken);
    }
}
