package com.example.OrderService.Service;

import com.example.OrderService.DTO.CustomerDTO;
import com.example.OrderService.Event.OrderPlacedEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.client.RestTemplate;


@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    private static final Logger logger = LoggerFactory.getLogger(KafkaProducerService.class);

    private final KafkaTemplate<String,OrderPlacedEvent> kafkaTemplate;

    private final RestTemplate restTemplate;

    @Value("${user.service.url}")
    private String userServiceUrl;

    private static final String TOPIC = "notification-group";

    public void sendOrderPlacedEvent(String accessToken,String userId,String orderId,double amount,String shippingAddress) {

        CustomerDTO customerDTO=getUserDetails(accessToken);
        OrderPlacedEvent orderPlacedEvent=new OrderPlacedEvent(orderId,
                                                               userId,
                                                               customerDTO.getFname(),
                                                               customerDTO.getPhone(),
                                                               customerDTO.getEmail(),
                                                               amount,
                                                               shippingAddress);
        logger.info("Sending OrderPlacedEvent to Kafka: {}",orderPlacedEvent);
        kafkaTemplate.send(TOPIC,orderPlacedEvent);
    }

    private CustomerDTO getUserDetails(String accessToken)
    {
        try {
            String url = userServiceUrl + "/getCustomerById";
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<CustomerDTO> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    CustomerDTO.class
            );
            return response.getBody();
        } catch (Exception e) {
            logger.error("Failed to fetch user details from user service : {}", e.getMessage());
            throw new RuntimeException("Failed to fetch user service");
        }
    }
}
