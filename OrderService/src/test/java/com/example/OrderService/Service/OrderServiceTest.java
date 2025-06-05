package com.example.OrderService.Service;

import com.example.OrderService.DTO.*;
import com.example.OrderService.Entity.Order;
import com.example.OrderService.Entity.OrderItem;
import com.example.OrderService.Enum.OrderStatus;
import com.example.OrderService.Enum.PaymentMode;
import com.example.OrderService.Enum.PaymentStatus;
import com.example.OrderService.Repository.OrderItemRepository;
import com.example.OrderService.Repository.OrderRepository;
import com.example.OrderService.Utility.TokenUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import java.util.*;

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
    private RestTemplate restTemplate;

    @InjectMocks
    private OrderService orderService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        orderService = new OrderService(orderRepository, orderItemRepository,
                kafkaProducerService, tokenUtil,
                restTemplate);

    }

    @Test
    void testPlaceOrder_Success() {
        String token = "valid-token";
        String userId = "user123";
        String orderId = UUID.randomUUID().toString();

        OrderDTO orderDTO = new OrderDTO();
        orderDTO.setShippingAddress("123 Test Street");
        orderDTO.setPaymentMode(PaymentMode.ONLINE);

        CartSummaryDTO cartSummaryDTO = new CartSummaryDTO();
        cartSummaryDTO.setTotalBill(999.99);
        List<CartProductDTO> cartProducts = List.of(
                new CartProductDTO("prod1", 2),
                new CartProductDTO("prod2", 1)
        );
        cartSummaryDTO.setProducts(cartProducts);

        List<ProductDTO> productDTOs = List.of(
                new ProductDTO("id1",100.0, "Product 1", 100),
                new ProductDTO("id2",150.0, "Product 2", 200)
        );

        when(tokenUtil.extractUserId(token)).thenReturn(userId);
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(CartSummaryDTO.class)
        )).thenReturn(new ResponseEntity<>(cartSummaryDTO, HttpStatus.OK));

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Boolean.class)
        )).thenReturn(new ResponseEntity<>(true, HttpStatus.OK));

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                ArgumentMatchers.<ParameterizedTypeReference<List<ProductDTO>>>any()
        )).thenReturn(new ResponseEntity<>(productDTOs, HttpStatus.OK));

        ResponseEntity<String> result = orderService.placeOrder(token, orderDTO);

        assertEquals("Order Placed Successfully", result.getBody());
        verify(orderRepository, times(1)).save(any(Order.class));
        verify(orderItemRepository, times(1)).saveAll(anyList());
        verify(kafkaProducerService, times(1)).sendOrderPlacedEvent(
                eq(token), eq(userId), anyString(), eq(999.99), eq("123 Test Street"));
    }

    @Test
    void testPlaceOrder_InvalidStock() {
        String token = "token";
        String userId = "user";

        CartSummaryDTO cartSummaryDTO = new CartSummaryDTO();
        cartSummaryDTO.setTotalBill(100.0);
        cartSummaryDTO.setProducts(List.of(new CartProductDTO("prod1", 1)));

        OrderDTO orderDTO = new OrderDTO();
        orderDTO.setShippingAddress("address");
        orderDTO.setPaymentMode(PaymentMode.ONLINE);

        when(tokenUtil.extractUserId(token)).thenReturn(userId);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(CartSummaryDTO.class)))
                .thenReturn(new ResponseEntity<>(cartSummaryDTO, HttpStatus.OK));
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Boolean.class)))
                .thenReturn(new ResponseEntity<>(false, HttpStatus.OK));

        ResponseEntity<String> result = orderService.placeOrder(token, orderDTO);

        assertEquals("Not enough stock/out of stock", result.getBody());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void testGetOrderHistory() {
        String token = "token";
        String userId = "user123";
        List<Order> mockOrders = List.of(new Order());

        when(tokenUtil.extractUserId(token)).thenReturn(userId);
        when(orderRepository.getOrderHistory(userId)).thenReturn(mockOrders);

        List<Order> result = orderService.getOrderHistory(token);
        assertEquals(1, result.size());
        verify(orderRepository, times(1)).getOrderHistory(userId);
    }

    @Test
    void testClearCart_Failure() {
        String token = "token";
        String userId = "user1";
        OrderDTO orderDTO = new OrderDTO();
        orderDTO.setShippingAddress("abc");
        orderDTO.setPaymentMode(PaymentMode.ONLINE);

        // Stub user ID
        when(tokenUtil.extractUserId(token)).thenReturn(userId);

        // Stub getCartSummary
        CartSummaryDTO cartSummary = new CartSummaryDTO();
        cartSummary.setTotalBill(500.0);
        cartSummary.setProducts(List.of(new CartProductDTO("prod1", 1)));
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(CartSummaryDTO.class)
        )).thenReturn(new ResponseEntity<>(cartSummary, HttpStatus.OK));

        // Stub isCartValid
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Boolean.class)
        )).thenReturn(new ResponseEntity<>(true, HttpStatus.OK));

        // Stub getCartProducts
        List<ProductDTO> productDTOs = List.of(new ProductDTO("prod1", 100.0, "Product 1", 10));
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                ArgumentMatchers.<ParameterizedTypeReference<List<ProductDTO>>>any()
        )).thenReturn(new ResponseEntity<>(productDTOs, HttpStatus.OK));

        // Now simulate failure in clearCart
        doThrow(new RuntimeException("Service unavailable")).when(restTemplate).exchange(
                anyString(),
                eq(HttpMethod.DELETE),
                any(HttpEntity.class),
                eq(String.class)
        );

        // Test
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            orderService.placeOrder(token, orderDTO);
        });

        assertTrue(exception.getMessage().contains("Failed to clear user cart"));
    }

}
