package com.example.OrderService.Service;

import com.example.OrderService.DTO.CustomerDTO;
import com.example.OrderService.Event.OrderPlacedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.kafka.core.KafkaTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class KafkaProducerServiceTest {

    @Mock
    private KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;

    @Mock
    private OrderInterServiceClient orderInterServiceClient;

    @InjectMocks
    private KafkaProducerService kafkaProducerService;

    @Captor
    private ArgumentCaptor<OrderPlacedEvent> eventCaptor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testSendOrderPlacedEvent_Success() {
        // Arrange
        String accessToken = "mock-token";
        String userId = "user-123";
        String orderId = "order-123";
        double amount = 500.0;
        String shippingAddress = "123 Main Street";

        CustomerDTO mockCustomer = new CustomerDTO();
        mockCustomer.setFname("John");
        mockCustomer.setPhone("1234567890");
        mockCustomer.setEmail("john@example.com");

        when(orderInterServiceClient.getUserDetails(accessToken)).thenReturn(mockCustomer);

        // Act
        kafkaProducerService.sendOrderPlacedEvent(accessToken, userId, orderId, amount, shippingAddress);

        // Assert
        verify(kafkaTemplate, times(1)).send(eq("notification-group"), eventCaptor.capture());

        OrderPlacedEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getOrderId()).isEqualTo(orderId);
        assertThat(capturedEvent.getUserId()).isEqualTo(userId);
        assertThat(capturedEvent.getAmount()).isEqualTo(amount);
        assertThat(capturedEvent.getShippingAddress()).isEqualTo(shippingAddress);
        assertThat(capturedEvent.getUserName()).isEqualTo("John");
        assertThat(capturedEvent.getPhoneNumber()).isEqualTo("1234567890");
        assertThat(capturedEvent.getEmail()).isEqualTo("john@example.com");
    }
}
