package com.ecommerce.InventoryService.Service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@RequiredArgsConstructor
public class InventoryInterServiceClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${product.service.url}")
    private String productServiceUrl;

    @Value("${interservice.api.key}")
    private String interServiceKey;

    private static final Logger logger = LoggerFactory.getLogger(InventoryInterServiceClient.class);

    public void deleteProductInProductService(String productId, String accessToken) {
        try {
            String url = productServiceUrl + "/deleteProductByProductId?productId=" + productId;
            webClientBuilder.build()
                    .delete()
                    .uri(url)
                    .header("Internal-API-Key", interServiceKey)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .toBodilessEntity()
                    .block(); // Synchronous

            logger.info("Product deleted in Product service Database");
        } catch (Exception e) {
            logger.error("Error deleting product in Product Service: {}", e.getMessage());
            throw new RuntimeException("Failed to delete product in Product Service");
        }
    }
}
