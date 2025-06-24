package com.example.OrderService.Service;

import com.example.OrderService.DTO.CartProductDTO;
import com.example.OrderService.DTO.CartSummaryDTO;
import com.example.OrderService.DTO.CustomerDTO;
import com.example.OrderService.DTO.ProductDTO;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OrderInterServiceClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${cart.service.url}")
    private String cartServiceUrl;

    @Value("${inventory.service.url}")
    private String inventoryServiceUrl;

    @Value("${user.service.url}")
    private String userServiceUrl;

    @Value("${interservice.api.key}")
    private String interServiceKey;

    private static final Logger logger = LoggerFactory.getLogger(OrderInterServiceClient.class);

    @CircuitBreaker(name = "cartService", fallbackMethod = "fallbackGetUserCart")
    public CartSummaryDTO getUserCart(String accessToken) {
        WebClient webClient = webClientBuilder.baseUrl(cartServiceUrl).build();

        return webClient.get()
                .uri("/getCartSummary")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(CartSummaryDTO.class)
                .block();
    }

    @CircuitBreaker(name = "cartService", fallbackMethod = "fallbackClearCart")
    public void clearCart(String accessToken) {
        WebClient webClient = webClientBuilder.baseUrl(cartServiceUrl).build();

        webClient.delete()
                .uri("/clearCart")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }

    @CircuitBreaker(name = "inventoryService", fallbackMethod = "fallbackIsCartValid")
    public Boolean isCartValid(List<CartProductDTO> cartProducts, String accessToken) {
        WebClient webClient = webClientBuilder.baseUrl(inventoryServiceUrl).build();

        return webClient.post()
                .uri("/isCartValid")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .header("Internal-API-Key", interServiceKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(cartProducts)
                .retrieve()
                .bodyToMono(Boolean.class)
                .block();
    }

    @CircuitBreaker(name = "inventoryService", fallbackMethod = "fallbackProductList")
    public List<ProductDTO> getCartProducts(String accessToken, List<String> productIds) {
        WebClient webClient = webClientBuilder.baseUrl(inventoryServiceUrl).build();

        return webClient.post()
                .uri("/getCartProducts")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .header("Internal-API-Key", interServiceKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(productIds)
                .retrieve()
                .bodyToFlux(ProductDTO.class)
                .collectList()
                .block();
    }

    @CircuitBreaker(name = "inventoryService", fallbackMethod = "fallbackGetUserDetails")
    public CustomerDTO getUserDetails(String accessToken) {
        try {
            WebClient webClient = webClientBuilder.baseUrl(userServiceUrl).build();

            return webClient.get()
                    .uri("/getCustomerById")
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .retrieve()
                    .bodyToMono(CustomerDTO.class)
                    .block();
        } catch (Exception e) {
            logger.error("Failed to fetch user details from user service: {}", e.getMessage());
            throw new RuntimeException("Failed to fetch user service");
        }
    }

    public CustomerDTO fallbackGetUserDetails(String accessToken,Throwable t)
    {
        logger.error("Fallback for getUserDetails:{}",t.getMessage());
        return new CustomerDTO();
    }

    public List<ProductDTO> fallbackProductList(String accessToken, List<String> productIds, Throwable t) {
        logger.error("Fallback for getCartProducts: {}", t.getMessage());
        return Collections.emptyList();
    }

    public void fallbackClearCart(String accessToken, Throwable t) {
        logger.error("Fallback for clearCart: {}", t.getMessage());
    }

    public CartSummaryDTO fallbackGetUserCart(String accessToken, Throwable t) {
        logger.error("Fallback for getUserCart: {}", t.getMessage());
        return new CartSummaryDTO(); // return empty cart
    }

    public Boolean fallbackIsCartValid(List<CartProductDTO> cartProducts, String accessToken, Throwable t) {
        logger.error("Fallback for isCartValid: {}", t.getMessage());
        return false;
    }
}
