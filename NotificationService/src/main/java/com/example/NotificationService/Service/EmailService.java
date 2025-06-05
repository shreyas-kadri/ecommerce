package com.example.NotificationService.Service;

import com.example.NotificationService.Event.OrderPlacedEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @KafkaListener(topics = "notification-group", groupId = "email-consumer-group", containerFactory = "kafkaListenerContainerFactory")
    public void sendOrderConfirmation(OrderPlacedEvent event) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(event.getEmail());
        message.setSubject("Order Confirmation - " + event.getOrderId());
        message.setText("Thank you " + event.getUserName() + " for your order of ₹" + event.getAmount() + ". We are processing it now.");
        mailSender.send(message);
        logger.info("Email sent to {}",event.getEmail());
    }
}

