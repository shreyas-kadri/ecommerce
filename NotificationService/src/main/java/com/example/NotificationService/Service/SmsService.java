package com.example.NotificationService.Service;

import com.example.NotificationService.Event.OrderPlacedEvent;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import com.twilio.type.PhoneNumber;

@Service
public class SmsService {

    @Value("${twilio.account.sid}")
    private String accountSid;

    @Value("${twilio.auth.token}")
    private String authToken;

    @Value("${twilio.phone.number}")
    private String fromNumber;

    private static final Logger logger = LoggerFactory.getLogger(SmsService.class);

    @PostConstruct
    public void init() {
        Twilio.init(accountSid, authToken);
    }

    @KafkaListener(topics = "notification-group", groupId = "sms-consumer-group", containerFactory = "kafkaListenerContainerFactory")
    public void sendOrderConfirmation(OrderPlacedEvent event) {
        String rawPhone = event.getPhoneNumber();

        // Add country code if missing (e.g., default to India: +91)
        String formattedPhone = rawPhone.startsWith("+") ? rawPhone : "+91" + rawPhone;

        String message = String.format("Your order %s of ₹%.2f has been placed successfully.", event.getOrderId(), event.getAmount());

        Message.creator(
                new PhoneNumber(formattedPhone),
                new PhoneNumber(fromNumber),
                message
        ).create();
        logger.info("SmS sent to {}",formattedPhone);
    }
}

