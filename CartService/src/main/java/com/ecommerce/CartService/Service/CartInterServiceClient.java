package com.ecommerce.CartService.Service;

import com.ecommerce.CartService.DTO.ProductDTO;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CartInterServiceClient {

    private final RestTemplate restTemplate;

    @Value("${inventory.service.url}")
    private String inventoryServiceUrl;

    @Value("${interservice.api.key}")
    private String interServiceKey;

    private static final Logger logger = LoggerFactory.getLogger(CartInterServiceClient.class);

    @CircuitBreaker(name = "inventoryService", fallbackMethod = "fallbackIsInStock")
    public boolean isInStock(ProductDTO productDTO, String accessToken)
    {
        //call inventory service to verify whether product is in stock
        String url = inventoryServiceUrl + "/isInStock?productId=" + productDTO.getProductId() + "&quantity=" + productDTO.getQuantity();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Internal-API-Key",interServiceKey);
        headers.setBearerAuth(accessToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<Boolean> response = restTemplate.exchange(url, HttpMethod.GET, entity, Boolean.class);
        return response.getBody() != null && response.getBody();
    }

    @CircuitBreaker(name = "inventoryService", fallbackMethod = "fallbackDoesProductExist")
    public boolean doesProductExists(String productId,String accessToken)
    {
        //call inventory service to verify whether product exists
        String url = inventoryServiceUrl + "/doesProductExist?productId=" + productId;
        HttpHeaders headers = new HttpHeaders();
        headers.set("Internal-API-Key",interServiceKey);
        headers.setBearerAuth(accessToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<Boolean> response = restTemplate.exchange(url, HttpMethod.GET, entity, Boolean.class);
        return response.getBody() != null && response.getBody();
    }

    @CircuitBreaker(name = "inventoryService", fallbackMethod = "fallbackGetTotalBill")
    public double getTotalBill(List<ProductDTO> cartProducts, String accessToken)
    {
        //call product service to get total bill
        String url = inventoryServiceUrl + "/getTotalBill";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Internal-API-Key",interServiceKey);
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<List<ProductDTO>> entity = new HttpEntity<>(cartProducts, headers);
        ResponseEntity<Double> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, Double.class);

        return response.getBody() != null ? response.getBody() : 0.0;
    }

    public boolean fallbackIsInStock(ProductDTO productDTO, String accessToken, Throwable t)
    {
        logger.warn("Fallback triggered for isInStock due to: {}", t.toString());
        return false; // decide if you want to fail open or closed
    }

    public boolean fallbackDoesProductExist(String productId, String accessToken, Throwable t)
    {
        logger.warn("Fallback triggered for doesProductExist due to: {}", t.toString());
        return false; // decide if you want to fail open or closed
    }

    public double fallbackGetTotalBill(List<ProductDTO> cartProducts, String accessToken, Throwable t)
    {
        logger.warn("Fallback triggered for getTotalBill due to: {}", t.toString());
        return -999.999; // decide if you want to fail open or closed
    }

}
