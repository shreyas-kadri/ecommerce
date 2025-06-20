package com.ecommerce.ProductService.Service;

import com.ecommerce.ProductService.DTO.NameAndPriceDTO;
import com.ecommerce.ProductService.Entity.Product;
import com.ecommerce.ProductService.Repository.ProductRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;


@Service
@RequiredArgsConstructor
public class ProductInterServiceClient {

    @Value("${inventory.service.url}")
    private String inventoryServiceUrl;

    @Value("${interservice.api.key}")
    private String interServiceKey;

    private final WebClient.Builder webClientBuilder;

    private final ProductRepository productRepository;

    private static final Logger logger = LoggerFactory.getLogger(ProductInterServiceClient.class);

    @CircuitBreaker(name = "inventoryService", fallbackMethod = "fallbackDoesProductExist")
    public boolean doesProductExist(String productId, String accessToken) {
        String url = inventoryServiceUrl + "/doesProductExist?productId=" + productId;

        Boolean response = webClientBuilder.build()
                .get()
                .uri(url)
                .headers(headers -> {
                    headers.set("Internal-API-Key", interServiceKey);
                    headers.setBearerAuth(accessToken);
                })
                .retrieve()
                .bodyToMono(Boolean.class)
                .block(); // blocking since service is not reactive

        return Boolean.TRUE.equals(response);
    }

    @CircuitBreaker(name = "inventoryService", fallbackMethod = "fallbackDoesSellerOwnProduct")
    public boolean doesSellerOwnProduct(String productId, String sellerId, String accessToken) {
        String url = inventoryServiceUrl + "/doesSellerOwnProduct?productId=" + productId + "&sellerId=" + sellerId;

        Boolean response = webClientBuilder.build()
                .get()
                .uri(url)
                .headers(headers -> {
                    headers.set("Internal-API-Key", interServiceKey);
                    headers.setBearerAuth(accessToken);
                })
                .retrieve()
                .bodyToMono(Boolean.class)
                .block();

        return Boolean.TRUE.equals(response);
    }

    @CircuitBreaker(name = "inventoryService", fallbackMethod = "fallbackFetchNameAndPrice")
    public NameAndPriceDTO fetchNameAndPrice(@NotBlank String productId, String accessToken) {
        String url = inventoryServiceUrl + "/fetchNameAndPrice?productId=" + productId;

        return webClientBuilder.build()
                .get()
                .uri(url)
                .headers(headers -> {
                    headers.set("Internal-API-Key", interServiceKey);
                    headers.setBearerAuth(accessToken);
                })
                .retrieve()
                .bodyToMono(NameAndPriceDTO.class)
                .block();
    }

    @CircuitBreaker(name = "inventoryService", fallbackMethod = "fallbackGetAllProductsBySellerId")
    public List<Product> getAllProductsBySellerId(String sellerId, String accessToken) {
        String url = inventoryServiceUrl + "/getProductIds/" + sellerId;

        List<String> productIds = webClientBuilder.build()
                .get()
                .uri(url)
                .headers(headers -> {
                    headers.set("Internal-API-Key", interServiceKey);
                    headers.setBearerAuth(accessToken);
                })
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<String>>() {})
                .block(); // Blocking because the rest of your codebase is synchronous

        return productRepository.findProductsByProductIds(productIds != null ? productIds : List.of());
    }

    public boolean fallbackDoesProductExist(String productId, String accessToken, Throwable t) {
        logger.warn("Fallback triggered for doesProductExist due to: {}", t.toString());
        return false;
    }

    public boolean fallbackDoesSellerOwnProduct(String productId, String sellerId, String accessToken, Throwable t) {
        logger.warn("Fallback triggered for doesSellerOwnProduct due to: {}", t.toString());
        return false;
    }

    public NameAndPriceDTO fallbackFetchNameAndPrice(String productId, String accessToken, Throwable t) {
        logger.warn("Fallback triggered for fetchNameAndPrice due to: {}", t.toString());
        return new NameAndPriceDTO();
    }

    public List<Product> fallbackGetAllProductsBySellerId(String sellerId,String accessToken,Throwable t)
    {
        logger.warn("Fallback triggered for getAllProductsBySellerId due to : {}",t.toString());
        return new ArrayList<>();
    }
}

