package com.example.OrderService.Service;

import com.example.OrderService.DTO.CartSummaryDTO;
import com.example.OrderService.DTO.OrderDTO;
import com.example.OrderService.DTO.CartProductDTO;
import com.example.OrderService.DTO.ProductDTO;
import com.example.OrderService.Entity.Order;
import com.example.OrderService.Entity.OrderItem;
import com.example.OrderService.Enum.OrderStatus;
import com.example.OrderService.Enum.PaymentStatus;
import com.example.OrderService.Event.OrderPlacedEvent;
import com.example.OrderService.Repository.OrderItemRepository;
import com.example.OrderService.Repository.OrderRepository;
import com.example.OrderService.Utility.TokenUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;

    private final OrderItemRepository orderItemRepository;

    private final KafkaProducerService kafkaProducerService;

    private final TokenUtil tokenUtil;

    private final RestTemplate restTemplate;

    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    @Value("${cart.service.url}")
    private String cartServiceUrl;

    @Value("${inventory.service.url}")
    private String inventoryServiceUrl;

    @Value("${interservice.api.key}")
    private String interServiceKey;

    public ResponseEntity<String> placeOrder(String accessToken, OrderDTO orderDTO)
    {
        String userId=tokenUtil.extractUserId(accessToken);
        CartSummaryDTO cartSummaryDTO=getUserCart(accessToken);

        List<CartProductDTO> productsInCart=cartSummaryDTO.getProducts();

        Boolean isCartValid=isCartValid(productsInCart,accessToken);

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

        List<ProductDTO> cartProducts=getCartProducts(accessToken,productIds);

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
        clearCart(accessToken);
        kafkaProducerService.sendOrderPlacedEvent(accessToken,userId,orderId,cartSummaryDTO.getTotalBill(),orderDTO.getShippingAddress());
        return ResponseEntity.ok("Order Placed Successfully");

    }

    private void clearCart(String accessToken)
    {
        try {
            String url = cartServiceUrl + "/clearCart";
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.DELETE,
                    entity,
                    String.class
            );
        } catch (Exception e) {
            logger.error("Error clearing cart in cart service: {}", e.getMessage());
            throw new RuntimeException("Failed to clear user cart from cart service");
        }
    }

    private CartSummaryDTO getUserCart(String accessToken)
    {
        try {
            String url = cartServiceUrl + "/getCartSummary";
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<CartSummaryDTO> response = restTemplate.exchange(url, HttpMethod.GET, entity,CartSummaryDTO.class);
            return response.getBody();
        }
        catch(Exception e)
        {
            logger.error("Error Fetching cart in cart service: {}", e.getMessage());
            throw new RuntimeException("Failed to fetch user cart from cart service");
        }
    }

    private Boolean isCartValid(List<CartProductDTO> cartProducts, String accessToken)
    {
        try {
            String url = inventoryServiceUrl + "/isCartValid";
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.set("Internal-API-Key",interServiceKey);
            HttpEntity<List<CartProductDTO>> entity = new HttpEntity<>(cartProducts,headers);
            ResponseEntity<Boolean> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    Boolean.class
            );
            return response.getBody();
        }
        catch(Exception e)
        {
            logger.error("Error Fetching cart in cart service: {}", e.getMessage());
            throw new RuntimeException("Failed to fetch user cart from cart service");
        }
    }

    private List<ProductDTO> getCartProducts(String accessToken, List<String> productIds)
    {
        try {
            String url = inventoryServiceUrl + "/getCartProducts";
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.set("Internal-API-Key",interServiceKey);
            HttpEntity<List<String>> entity = new HttpEntity<>(productIds,headers);
            ResponseEntity<List<ProductDTO>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<List<ProductDTO>>() {}
            );
            return response.getBody();
        }
        catch(Exception e)
        {
            logger.error("Error Fetching cart in cart service: {}", e.getMessage());
            throw new RuntimeException("Failed to fetch user cart from cart service");
        }
    }

    public List<Order> getOrderHistory(String accessToken)
    {
        String userId=tokenUtil.extractUserId(accessToken);
        return orderRepository.getOrderHistory(userId);
    }
}
