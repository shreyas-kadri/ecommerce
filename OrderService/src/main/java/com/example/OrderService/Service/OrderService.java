package com.example.OrderService.Service;

import com.example.OrderService.DTO.CartSummaryDTO;
import com.example.OrderService.DTO.OrderDTO;
import com.example.OrderService.DTO.CartProductDTO;
import com.example.OrderService.DTO.ProductDTO;
import com.example.OrderService.Entity.Order;
import com.example.OrderService.Entity.OrderItem;
import com.example.OrderService.Enum.OrderStatus;
import com.example.OrderService.Enum.PaymentStatus;
import com.example.OrderService.Repository.OrderItemRepository;
import com.example.OrderService.Repository.OrderRepository;
import com.example.OrderService.Utility.TokenUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final KafkaProducerService kafkaProducerService;
    private final TokenUtil tokenUtil;
    private final OrderInterServiceClient  orderInterServiceClient;

    public ResponseEntity<String> placeOrder(String accessToken, OrderDTO orderDTO)
    {
        String userId=tokenUtil.extractUserId(accessToken);
        CartSummaryDTO cartSummaryDTO=orderInterServiceClient.getUserCart(accessToken);

        List<CartProductDTO> productsInCart=cartSummaryDTO.getProducts();

        Boolean isCartValid=orderInterServiceClient.isCartValid(productsInCart,accessToken);

        if(!isCartValid)
        {
            return ResponseEntity.ok("Not enough stock/out of stock");
        }
        Order order = new Order();
        String orderId=UUID.randomUUID().toString();
        order.setOrderId(orderId);
        order.setUserId(userId);
        order.setOrderPlacedTime(BigInteger.valueOf(System.currentTimeMillis()));
        order.setShippingAddress(orderDTO.getShippingAddress());
        order.setPaymentMode(orderDTO.getPaymentMode());
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setOrderStatus(OrderStatus.PLACED);
        order.setAmount(cartSummaryDTO.getTotalBill());
        orderRepository.save(order);

        List<String> productIds = cartSummaryDTO.getProducts()
                .stream()
                .map(CartProductDTO::getProductId)
                .collect(Collectors.toList());

        List<ProductDTO> cartProducts=orderInterServiceClient.getCartProducts(accessToken,productIds);

        List<OrderItem> orderItems=new ArrayList<>();

        for(ProductDTO productDTO:cartProducts)
        {
            OrderItem orderItem=new OrderItem();
            orderItem.setOrderItemId(UUID.randomUUID().toString());
            orderItem.setProductId(productDTO.getProductId());
            orderItem.setPrice(productDTO.getPrice());
            orderItem.setName(productDTO.getName());
            orderItems.add(orderItem);
            orderItem.setOrder(order);
        }

        orderItems.forEach(orderItem ->
                productsInCart.stream()
                        .filter(cartProduct -> cartProduct.getProductId().equals(orderItem.getProductId()))
                        .findFirst()
                        .ifPresent(cartProduct -> orderItem.setQuantity(cartProduct.getQuantity()))
        );

        orderItemRepository.saveAll(orderItems);
        orderInterServiceClient.clearCart(accessToken);
        kafkaProducerService.sendOrderPlacedEvent(accessToken,userId,orderId,cartSummaryDTO.getTotalBill(),orderDTO.getShippingAddress());
        return ResponseEntity.ok("Order Placed Successfully");

    }

    public List<Order> getOrderHistory(String accessToken) {
        String userId = tokenUtil.extractUserId(accessToken);
        return orderRepository.getOrderHistory(userId);
    }
}

