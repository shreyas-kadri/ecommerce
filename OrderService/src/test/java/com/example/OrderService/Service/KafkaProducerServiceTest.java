package com.example.OrderService.Service;

import com.example.OrderService.DTO.CustomerDTO;
import com.example.OrderService.Event.OrderPlacedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.*;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class KafkaProducerServiceTest {

    @Mock
    private KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private KafkaProducerService kafkaProducerService;

    private final String USER_SERVICE_URL = "http://fake-user-service";

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        // Inject userServiceUrl via reflection
        Field field = KafkaProducerService.class.getDeclaredField("userServiceUrl");
        field.setAccessible(true);
        field.set(kafkaProducerService, USER_SERVICE_URL);
    }

    @Test
    void testSendOrderPlacedEvent_Success() {
        // Arrange
        String token = "token";
        String userId = "user1";
        String orderId = "order123";
        double amount = 99.99;
        String shippingAddress = "123 Test St";

        CustomerDTO mockCustomer = new CustomerDTO();
        mockCustomer.setFname("John");
        mockCustomer.setPhone("1234567890");
        mockCustomer.setEmail("john@example.com");

        when(restTemplate.exchange(
                eq(USER_SERVICE_URL + "/getCustomerById"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(CustomerDTO.class)
        )).thenReturn(new ResponseEntity<>(mockCustomer, HttpStatus.OK));

        // Act
        kafkaProducerService.sendOrderPlacedEvent(token, userId, orderId, amount, shippingAddress);

        // Assert
        ArgumentCaptor<OrderPlacedEvent> eventCaptor = ArgumentCaptor.forClass(OrderPlacedEvent.class);
        verify(kafkaTemplate).send(eq("notification-group"), eventCaptor.capture());

        OrderPlacedEvent event = eventCaptor.getValue();
        assertEquals(orderId, event.getOrderId());
        assertEquals(userId, event.getUserId());
        assertEquals("John", event.getUserName());
        assertEquals("1234567890", event.getPhoneNumber());
        assertEquals("john@example.com", event.getEmail());
        assertEquals(amount, event.getAmount());
        assertEquals(shippingAddress, event.getShippingAddress());
    }

    @Test
    void testSendOrderPlacedEvent_FailureOnUserService() {
        // Arrange
        String token = "token";

        when(restTemplate.exchange(
                eq(USER_SERVICE_URL + "/getCustomerById"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(CustomerDTO.class)
        )).thenThrow(new RuntimeException("User service down"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                kafkaProducerService.sendOrderPlacedEvent(token, "user1", "order123", 50.0, "Somewhere"));

        assertEquals("Failed to fetch user service", exception.getMessage());
        verify(kafkaTemplate, never()).send(anyString(), any(OrderPlacedEvent.class));
    }
}
