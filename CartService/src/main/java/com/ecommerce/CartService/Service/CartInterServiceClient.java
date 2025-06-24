package com.ecommerce.CartService.Service;

import com.ecommerce.CartService.DTO.ProductDTO;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CartInterServiceClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${inventory.service.url}")
    private String inventoryServiceUrl;

    @Value("${interservice.api.key}")
    private String interServiceKey;

    private static final Logger logger = LoggerFactory.getLogger(CartInterServiceClient.class);

    @CircuitBreaker(name = "inventoryService", fallbackMethod = "fallbackIsInStock")
    public boolean isInStock(ProductDTO productDTO, String accessToken)
    {
        Boolean result = webClientBuilder.build()
                    .get()
                    .uri(inventoryServiceUrl + "/isInStock?productId={productId}&quantity={quantity}", productDTO.getProductId(), productDTO.getQuantity())
                    .header("Internal-API-Key", interServiceKey)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(Boolean.class)
                    .block();
        return result != null && result;
    }

    @CircuitBreaker(name = "inventoryService", fallbackMethod = "fallbackDoesProductExist")
    public boolean doesProductExists(String productId, String accessToken)
    {
        Boolean result = webClientBuilder.build()
                    .get()
                    .uri(inventoryServiceUrl + "/doesProductExist?productId={productId}", productId)
                    .header("Internal-API-Key", interServiceKey)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(Boolean.class)
                    .block();
        return result != null && result;
    }

    @CircuitBreaker(name = "inventoryService", fallbackMethod = "fallbackGetTotalBill")
    public double getTotalBill(List<ProductDTO> cartProducts, String accessToken)
    {
        Double totalBill = webClientBuilder.build()
                    .post()
                    .uri(inventoryServiceUrl + "/getTotalBill")
                    .header("Internal-API-Key", interServiceKey)
                    .header("Authorization", "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(cartProducts)
                    .retrieve()
                    .bodyToMono(Double.class)
                    .block();
        return totalBill != null ? totalBill : 0.0;
    }

    public boolean fallbackIsInStock(ProductDTO productDTO, String accessToken, Throwable t)
    {
        logger.warn("Fallback triggered for isInStock due to: {}", t.toString());
        return false;
    }

    public boolean fallbackDoesProductExist(String productId, String accessToken, Throwable t)
    {
        logger.warn("Fallback triggered for doesProductExist due to: {}", t.toString());
        return false;
    }

    public double fallbackGetTotalBill(List<ProductDTO> cartProducts, String accessToken, Throwable t)
    {
        logger.warn("Fallback triggered for getTotalBill due to: {}", t.toString());
        return -999.999;
    }
}
