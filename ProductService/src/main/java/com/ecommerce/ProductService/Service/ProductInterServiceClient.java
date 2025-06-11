package com.ecommerce.ProductService.Service;

import com.ecommerce.ProductService.DTO.NameAndPriceDTO;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;


@Service
@RequiredArgsConstructor
public class ProductInterServiceClient {

    @Value("${inventory.service.url}")
    private String inventoryServiceUrl;

    @Value("${interservice.api.key}")
    private String interServiceKey;

    private final RestTemplate restTemplate;

    private static final Logger logger = LoggerFactory.getLogger(ProductInterServiceClient.class);

    @CircuitBreaker(name = "inventoryService", fallbackMethod = "fallbackDoesProductExist")
    public boolean doesProductExist(String productId,String accessToken)
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

    @CircuitBreaker(name = "inventoryService", fallbackMethod = "fallbackDoesSellerOwnProduct")
    public boolean doesSellerOwnProduct(String productId,String sellerId,String accessToken)
    {
        //call inventory service to verify whether product exists
        String url = inventoryServiceUrl + "/doesSellerOwnProduct?productId=" + productId + "&sellerId=" + sellerId;
        HttpHeaders headers = new HttpHeaders();
        headers.set("Internal-API-Key",interServiceKey);
        headers.setBearerAuth(accessToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<Boolean> response = restTemplate.exchange(url, HttpMethod.GET, entity, Boolean.class);
        return response.getBody() != null && response.getBody();
    }

    @CircuitBreaker(name = "inventoryService", fallbackMethod = "fallbackFetchNameAndPrice")
    public NameAndPriceDTO fetchNameAndPrice(@NotBlank(message = "Product id is required") String productId, String accessToken)
    {
        String url = inventoryServiceUrl + "/fetchNameAndPrice?productId=" + productId;
        HttpHeaders headers = new HttpHeaders();
        headers.set("Internal-API-Key",interServiceKey);
        headers.setBearerAuth(accessToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<NameAndPriceDTO> response = restTemplate.exchange(url, HttpMethod.GET, entity,NameAndPriceDTO.class);
        return response.getBody();
    }

    public boolean fallbackDoesProductExist(String productId, String accessToken, Throwable t) {
        logger.warn("Fallback triggered for doesProductExist due to: {}", t.toString());
        return false; // decide if you want to fail open or closed
    }

    public boolean fallbackDoesSellerOwnProduct(String productId, String sellerId, String accessToken, Throwable t) {
        logger.warn("Fallback triggered for doesSellerOwnProduct due to: {}", t.toString());
        return false; // same, based on how critical ownership check is
    }

    public NameAndPriceDTO fallbackFetchNameAndPrice(@NotBlank(message = "Product id is required") String productId, String accessToken,Throwable t)
    {
        logger.warn("Fallback triggered for fetchNameAndPrice due to : {} ",t.toString());
        return new NameAndPriceDTO();
    }
}
