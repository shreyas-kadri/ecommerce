package com.example.OrderService.Service;

import com.example.OrderService.DTO.CustomerDTO;
import com.example.OrderService.Event.OrderPlacedEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    private static final Logger logger = LoggerFactory.getLogger(KafkaProducerService.class);

    private final KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;

    private final OrderInterServiceClient  orderInterServiceClient;

    private static final String TOPIC = "notification-group";

    public void sendOrderPlacedEvent(String accessToken, String userId, String orderId, double amount, String shippingAddress) {
        CustomerDTO customerDTO = orderInterServiceClient.getUserDetails(accessToken);
        OrderPlacedEvent orderPlacedEvent = new OrderPlacedEvent(
                orderId,
                userId,
                customerDTO.getFname(),
                customerDTO.getPhone(),
                customerDTO.getEmail(),
                amount,
                shippingAddress
        );
        logger.info("Sending OrderPlacedEvent to Kafka: {}", orderPlacedEvent);
        kafkaTemplate.send(TOPIC, orderPlacedEvent);
    }

}
