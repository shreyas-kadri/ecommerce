package com.example.OrderService.Service;

import com.example.OrderService.DTO.CartProductDTO;
import com.example.OrderService.DTO.CartSummaryDTO;
import com.example.OrderService.DTO.ProductDTO;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OrderInterServiceClient {

    private final RestTemplate restTemplate;

    @Value("${cart.service.url}")
    private String cartServiceUrl;

    @Value("${inventory.service.url}")
    private String inventoryServiceUrl;

    @Value("${interservice.api.key}")
    private String interServiceKey;

    private static final Logger logger = LoggerFactory.getLogger(OrderInterServiceClient.class);


    @CircuitBreaker(name = "cartService", fallbackMethod = "fallbackGetUserCart")
    public CartSummaryDTO getUserCart(String accessToken)
    {
        String url = cartServiceUrl + "/getCartSummary";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<CartSummaryDTO> response = restTemplate.exchange(url, HttpMethod.GET, entity,CartSummaryDTO.class);
        return response.getBody();
    }

    @CircuitBreaker(name = "cartService", fallbackMethod = "fallbackVoid")
    public void clearCart(String accessToken)
    {
        String url = cartServiceUrl + "/clearCart";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.DELETE,
                    entity,
                    String.class
        );
    }

    @CircuitBreaker(name = "inventoryService", fallbackMethod = "fallbackIsCartValid")
    public Boolean isCartValid(List<CartProductDTO> cartProducts, String accessToken)
    {
        String url = inventoryServiceUrl + "/isCartValid";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.set("Internal-API-Key",interServiceKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<List<CartProductDTO>> entity = new HttpEntity<>(cartProducts,headers);
        ResponseEntity<Boolean> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    Boolean.class
            );
        return response.getBody();
    }

    @CircuitBreaker(name = "inventoryService", fallbackMethod = "fallbackProductList")
    public List<ProductDTO> getCartProducts(String accessToken, List<String> productIds)
    {
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

    public List<ProductDTO> fallbackProductList(String accessToken, List<String> productIds, Throwable t)
    {
        logger.error("Fallback for getCartProducts: {}", t.getMessage());
        return Collections.emptyList();
    }

    public void fallbackVoid(String accessToken, Throwable t)
    {
        logger.error("Fallback for clearCart: {}", t.getMessage());
    }

    public CartSummaryDTO fallbackGetUserCart(String accessToken, Throwable t)
    {
        logger.error("Fallback for getUserCart: {}", t.getMessage());
        return new CartSummaryDTO(); // return empty cart
    }

    public Boolean fallbackIsCartValid(List<CartProductDTO> cartProducts, String accessToken, Throwable t)
    {
        logger.error("Fallback for isCartValid: {}", t.getMessage());
        return false;
    }

}

