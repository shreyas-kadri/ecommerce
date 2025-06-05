package com.ecommerce.ProductService.Service;

import com.ecommerce.ProductService.DTO.NameAndPriceDTO;
import com.ecommerce.ProductService.DTO.ProductDTO;
import com.ecommerce.ProductService.Entity.Product;
import com.ecommerce.ProductService.Repository.ProductRepository;
import com.ecommerce.ProductService.Utility.TokenUtil;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    private final TokenUtil tokenUtil;

    private final RestTemplate restTemplate;

    @Value("${inventory.service.url}")
    private String inventoryServiceUrl;

    @Value("${interservice.api.key}")
    private String interServiceKey;

    private static final Logger logger = LoggerFactory.getLogger(ProductService.class);

    @CachePut(value="products",key="#productDTO.productId")
    public ResponseEntity<String> createProduct(@Valid ProductDTO productDTO, String accessToken)
    {
        String sellerId=tokenUtil.extractUserId(accessToken);
        if(!doesProductExist(productDTO.getProductId(),accessToken))
        {
            logger.info("Product does not exist");
            throw new RuntimeException("Product does not exist");
        }
        if(!doesSellerOwnProduct(productDTO.getProductId(),sellerId,accessToken))
        {
            logger.info(String.format("Seller with ID %s does not own this product with ID %s",sellerId,productDTO.getProductId()));
            throw new RuntimeException("Seller does not own product");
        }
        NameAndPriceDTO nameAndPrice = fetchNameAndPrice(productDTO.getProductId(),accessToken);
        Product product=Product.builder()
                               .productId(productDTO.getProductId())
                               .name(nameAndPrice.getName())
                               .price(nameAndPrice.getPrice())
                               .description(productDTO.getDescription())
                               .category(productDTO.getCategory())
                               .details(productDTO.getDetails())
                               .build();
        productRepository.save(product);
        return ResponseEntity.ok("Product created successfully");
    }

    @CachePut(value = "products", key = "#productDTO.productId")
    public ResponseEntity<String> updateProduct(@Valid ProductDTO productDTO, String accessToken)
    {
        String sellerId = tokenUtil.extractUserId(accessToken);

        if (!doesProductExist(productDTO.getProductId(), accessToken)) {
            logger.info("Product does not exist");
            throw new RuntimeException("Product does not exist");
        }

        if (!doesSellerOwnProduct(productDTO.getProductId(), sellerId, accessToken)) {
            logger.info(String.format("Seller with ID %s does not own this product with ID %s", sellerId, productDTO.getProductId()));
            throw new RuntimeException("Seller does not own product");
        }

        // Fetch existing product from DB to update
        Product existingProduct = productRepository.findById(productDTO.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        // Update mutable fields
        existingProduct.setDescription(productDTO.getDescription());
        existingProduct.setCategory(productDTO.getCategory());
        existingProduct.setDetails(productDTO.getDetails());

        // Save updated product
        productRepository.save(existingProduct);

        return ResponseEntity.ok("Product updated successfully");
    }


    private NameAndPriceDTO fetchNameAndPrice(@NotBlank(message = "Product id is required") String productId,String accessToken)
    {
        try {
            String url = inventoryServiceUrl + "/fetchNameAndPrice?productId=" + productId;
            HttpHeaders headers = new HttpHeaders();
            headers.set("Internal-API-Key",interServiceKey);
            headers.setBearerAuth(accessToken);
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<NameAndPriceDTO> response = restTemplate.exchange(url, HttpMethod.GET, entity,NameAndPriceDTO.class);
            return response.getBody();
        }
        catch(Exception e)
        {
            throw new RuntimeException(e.getMessage());
        }
    }

    public boolean doesProductExist(String productId,String accessToken)
    {
        //call inventory service to verify whether product exists
        try {
            String url = inventoryServiceUrl + "/doesProductExist?productId=" + productId;
            HttpHeaders headers = new HttpHeaders();
            headers.set("Internal-API-Key",interServiceKey);
            headers.setBearerAuth(accessToken);
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<Boolean> response = restTemplate.exchange(url, HttpMethod.GET, entity, Boolean.class);
            return response.getBody() != null && response.getBody();
        }
        catch(Exception e)
        {
            throw new RuntimeException(e.getMessage());
        }
    }

    public boolean doesSellerOwnProduct(String productId,String sellerId,String accessToken)
    {
        //call inventory service to verify whether product exists
        try {
            String url = inventoryServiceUrl + "/doesSellerOwnProduct?productId=" + productId + "&sellerId=" + sellerId;
            HttpHeaders headers = new HttpHeaders();
            headers.set("Internal-API-Key",interServiceKey);
            headers.setBearerAuth(accessToken);
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<Boolean> response = restTemplate.exchange(url, HttpMethod.GET, entity, Boolean.class);
            return response.getBody() != null && response.getBody();
        }
        catch(Exception e)
        {
            throw new RuntimeException(e.getMessage());
        }
    }

    @CacheEvict(value="products",key="#productId")
    public void deleteProduct(String productId)
    {
        productRepository.deleteById(productId);
    }

    @Cacheable(value="products",key="#productId")
    public Optional<Product> getProductByProductId(String productId)
    {
        return productRepository.findById(productId);
    }

    @Cacheable(value="products",key="'all'")
    public List<Product> getAllProducts()
    {
        return productRepository.findAll();
    }

    public List<Product> getAllProductsBySellerId(String sellerId,String accessToken)
    {
        //call inventory service to verify whether product exists
        try {
            String url = inventoryServiceUrl + "/getProductIds/" + sellerId;
            HttpHeaders headers = new HttpHeaders();
            headers.set("Internal-API-Key",interServiceKey);
            headers.setBearerAuth(accessToken);
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<List<String>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<String>>() {}
            );
            List<String> productIds=response.getBody();
            return productRepository.findProductsByProductIds(productIds);
        }
        catch(Exception e)
        {
            throw new RuntimeException(e.getMessage());
        }
    }
}
