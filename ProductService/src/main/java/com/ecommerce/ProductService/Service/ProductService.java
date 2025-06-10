package com.ecommerce.ProductService.Service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
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
import org.springframework.cache.annotation.Caching;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    private final TokenUtil tokenUtil;

    private final RestTemplate restTemplate;

    private final Cloudinary cloudinary;

    @Value("${inventory.service.url}")
    private String inventoryServiceUrl;

    @Value("${interservice.api.key}")
    private String interServiceKey;

    private static final Logger logger = LoggerFactory.getLogger(ProductService.class);

    @CachePut(value="products",key="#productDTO.productId")
    public ResponseEntity<String> createProduct(@Valid ProductDTO productDTO, String accessToken)
    {
        validateProductOwnershipAndExistence(productDTO.getProductId(),accessToken);
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
        validateProductOwnershipAndExistence(productDTO.getProductId(),accessToken);
        if(productRepository.findById(productDTO.getProductId()).isEmpty())
        {
            logger.info("Product does not exist with ID: {}", productDTO.getProductId());
            throw new RuntimeException("Product not found in DB with ID: " + productDTO.getProductId());
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

    @Caching(evict = {
            @CacheEvict(value = "products", key = "#productId"),   // Evict specific product
            @CacheEvict(value = "products", key = "'all'")         // Evict all products list
    })
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

    @Caching(evict = {
            @CacheEvict(value = "products", key = "#productId"),
            @CacheEvict(value = "products", key = "'all'")
    })
    public List<String> uploadProductImages(String accessToken,String productId, MultipartFile[] imageFiles) throws IOException
    {
        validateProductOwnershipAndExistence(productId,accessToken);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with ID: " + productId));
        List<String> uploadedUrls = new ArrayList<>();
        for (MultipartFile file : imageFiles) {
            Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(),
                    ObjectUtils.asMap("folder", "ecommerce/products/" + productId));

            String imageUrl = (String) uploadResult.get("secure_url");
            uploadedUrls.add(imageUrl);
        }

        if (product.getImageUrls() == null)
        {
            product.setImageUrls(new ArrayList<>());
        }

        product.getImageUrls().addAll(uploadedUrls);
        productRepository.save(product);

        return uploadedUrls;
    }

    @Caching(evict = {
            @CacheEvict(value = "products", key = "#productId"),   // Evict specific product
            @CacheEvict(value = "products", key = "'all'")         // Evict all products list
    })
    public List<String> deleteProductImages(String accessToken, String productId, List<String> imageUrls)
    {
        validateProductOwnershipAndExistence(productId,accessToken);
        Optional<Product> optionalProduct=productRepository.findById(productId);
        Product product=optionalProduct.get();
        List<String> images=product.getImageUrls();
        List<String> deletedImages = new ArrayList<>();
        for (String url : imageUrls) {
            try {
                // Extract public ID from the URL
                String publicId = extractPublicId(url);
                cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
                deletedImages.add(url);
            } catch (Exception e) {
                throw new RuntimeException("Failed to delete image: " + url, e);
            }
        }
        images.removeAll(deletedImages);
        product.setImageUrls(images);
        productRepository.save(product);
        return deletedImages;
    }

    private String extractPublicId(String url) {
        // Example URL: https://res.cloudinary.com/demo/image/upload/v1234567890/folder/image-name.jpg
        String[] parts = url.split("/");
        String publicIdWithExtension = parts[parts.length - 1]; // image-name.jpg
        String folder = parts[parts.length - 2];                // folder name (optional)
        String imageName = publicIdWithExtension.split("\\.")[0]; // image-name
        return "ecommerce/products/" + folder + "/" + imageName; // Adjust to your folder structure
    }

    private void validateProductOwnershipAndExistence(String productId, String accessToken)
    {
        String sellerId = tokenUtil.extractUserId(accessToken);

        if (!doesProductExist(productId, accessToken)) {
            logger.info("Product does not exist with ID: {}", productId);
            throw new RuntimeException("Product does not exist");
        }

        if (!doesSellerOwnProduct(productId, sellerId, accessToken))
        {
            logger.info("Seller with ID {} does not own product with ID {}", sellerId, productId);
            throw new RuntimeException("Seller does not own product");
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

    public List<Product> getAllProductsBySellerId(String sellerId,String accessToken)
    {

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
