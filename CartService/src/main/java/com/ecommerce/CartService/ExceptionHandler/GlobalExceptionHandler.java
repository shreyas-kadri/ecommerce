package com.ecommerce.CartService.ExceptionHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handleRuntimeException(RuntimeException ex, Model model) {
        // Log the exception (optional)
        logger.info("Exception: {}",ex.getMessage());
        // Return a custom message with a 404 Not Found status
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
    }
}