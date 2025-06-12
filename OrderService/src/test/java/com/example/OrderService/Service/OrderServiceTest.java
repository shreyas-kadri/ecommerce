package com.example.OrderService.Service;

import com.example.OrderService.DTO.*;
import com.example.OrderService.Entity.Order;
import com.example.OrderService.Enum.PaymentMode;
import com.example.OrderService.Repository.OrderItemRepository;
import com.example.OrderService.Repository.OrderRepository;
import com.example.OrderService.Utility.TokenUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private KafkaProducerService kafkaProducerService;

    @Mock
    private TokenUtil tokenUtil;

    @Mock
    private OrderInterServiceClient orderInterServiceClient;

    @InjectMocks
    private OrderService orderService;

    private String token;
    private String userId;

    @BeforeEach
    void setUp() {
        token = "dummy-token";
        userId = "user123";
    }

    @Test
    void testPlaceOrder_Success() {
        OrderDTO orderDTO = new OrderDTO();
        orderDTO.setShippingAddress("Some Address");
        orderDTO.setPaymentMode(PaymentMode.CASH_ON_DELIVERY);

        List<CartProductDTO> cartProducts = List.of(
                new CartProductDTO("prod1", 2),
                new CartProductDTO("prod2", 1)
        );

        CartSummaryDTO cartSummaryDTO = new CartSummaryDTO();
        cartSummaryDTO.setProducts(cartProducts);
        cartSummaryDTO.setTotalBill(500.0);

        List<ProductDTO> productDTOs = List.of(
                new ProductDTO("prod1", 100.0, "Product 1", 50),
                new ProductDTO("prod2", 150.0, "Product 2", 30)
        );

        when(tokenUtil.extractUserId(token)).thenReturn(userId);
        when(orderInterServiceClient.getUserCart(token)).thenReturn(cartSummaryDTO);
        when(orderInterServiceClient.isCartValid(cartProducts, token)).thenReturn(true);
        when(orderInterServiceClient.getCartProducts(token, List.of("prod1", "prod2"))).thenReturn(productDTOs);

        ResponseEntity<String> response = orderService.placeOrder(token, orderDTO);

        assertEquals("Order Placed Successfully", response.getBody());
        verify(orderRepository).save(any(Order.class));
        verify(orderItemRepository).saveAll(anyList());
        verify(orderInterServiceClient).clearCart(token);
        verify(kafkaProducerService).sendOrderPlacedEvent(
                eq(token), eq(userId), anyString(), eq(500.0), eq("Some Address"));
    }

    @Test
    void testPlaceOrder_InsufficientStock() {
        OrderDTO orderDTO = new OrderDTO();
        orderDTO.setShippingAddress("Test");
        orderDTO.setPaymentMode(PaymentMode.ONLINE);

        List<CartProductDTO> cartProducts = List.of(new CartProductDTO("prod1", 3));
        CartSummaryDTO cartSummaryDTO = new CartSummaryDTO();
        cartSummaryDTO.setProducts(cartProducts);
        cartSummaryDTO.setTotalBill(300.0);

        when(tokenUtil.extractUserId(token)).thenReturn(userId);
        when(orderInterServiceClient.getUserCart(token)).thenReturn(cartSummaryDTO);
        when(orderInterServiceClient.isCartValid(cartProducts, token)).thenReturn(false);

        ResponseEntity<String> response = orderService.placeOrder(token, orderDTO);

        assertEquals("Not enough stock/out of stock", response.getBody());
        verify(orderRepository, never()).save(any());
        verify(orderItemRepository, never()).saveAll(anyList());
        verify(orderInterServiceClient, never()).clearCart(any());
    }

    @Test
    void testGetOrderHistory() {
        List<Order> expectedOrders = List.of(new Order(), new Order());

        when(tokenUtil.extractUserId(token)).thenReturn(userId);
        when(orderRepository.getOrderHistory(userId)).thenReturn(expectedOrders);

        List<Order> result = orderService.getOrderHistory(token);

        assertEquals(2, result.size());
        verify(orderRepository).getOrderHistory(userId);
    }
}
