package com.ecommerce.InventoryService.Service;

import com.ecommerce.InventoryService.DTO.CartProductDTO;
import com.ecommerce.InventoryService.DTO.ProductDTO;
import com.ecommerce.InventoryService.Entity.Product;
import com.ecommerce.InventoryService.Repository.InventoryRepository;
import com.ecommerce.InventoryService.Utility.TokenUtil;
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

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    private final TokenUtil tokenUtil;

    private final InventoryInterServiceClient inventoryInterServiceClient;

    @Value("${product.service.url}")
    private String productServiceUrl;

    @Value("${interservice.api.key}")
    private String interServiceKey;

    private static final Logger logger = LoggerFactory.getLogger(InventoryService.class);

    private static final String PRODUCT_NOT_FOUND="Product not found";

    // Create a new product from DTO
    public ProductDTO createProduct(ProductDTO productDTO,String accessToken) {

        String sellerId=tokenUtil.extractUserId(accessToken);
        // Convert DTO to Entity
        Product product = Product.builder()
                .sellerId(sellerId)
                .name(productDTO.getName())
                .price(productDTO.getPrice())
                .stock(productDTO.getStock())
                .build();

        // Save product and return the DTO
        Product savedProduct = inventoryRepository.save(product);
        logger.info("Product saved successfully - Product ID:{}",product.getProductId());
        return savedProduct.toDTO();
    }

    public List<ProductDTO> createMultipleProducts(List<ProductDTO> productDTOs,String accessToken)
    {
        String sellerId=tokenUtil.extractUserId(accessToken);
        List<Product> products = productDTOs.stream()
                .map(productDTO -> Product.builder()
                        .sellerId(sellerId)
                        .name(productDTO.getName())
                        .price(productDTO.getPrice())
                        .stock(productDTO.getStock())
                        .build())
                .collect(Collectors.toList());

        List<Product> savedProducts = inventoryRepository.saveAll(products);
        logger.info("Products saved successfully");

        return savedProducts.stream()
                .map(product -> product.toDTO())
                .collect(Collectors.toList());
    }

    // Get all products and return a list of DTOs
    public List<ProductDTO> getAllProducts(String accessToken)
    {
        String sellerId = tokenUtil.extractUserId(accessToken);
        logger.info("Fetching products for Seller ID: {}", sellerId);

        return inventoryRepository.findAllProductsBySellerId(sellerId)
                .stream()
                .map(Product::toDTO)
                .collect(Collectors.toList());
    }

    // Get a single product by id and return a DTO
    public ProductDTO getProductById(String productId,String accessToken)
    {
        String sellerId=tokenUtil.extractUserId(accessToken);
        if (!doesProductExist(productId)) {
            throw new RuntimeException(PRODUCT_NOT_FOUND);
        }

        if(!doesSellerOwnProduct(productId,sellerId))
        {
            throw new RuntimeException(String.format("Product with ID %s does not belong to Seller with ID %s", productId, sellerId));
        }

        Product product = inventoryRepository.findProductBySellerId(productId,sellerId)
                .orElseThrow(() -> new RuntimeException(PRODUCT_NOT_FOUND));
        logger.info("Product fetched successfully - Product ID:{}",productId);
        return product.toDTO();
    }

    // Update a product by id using DTO
    public ProductDTO updateProduct(String productId, ProductDTO updatedProductDTO,String accessToken) {

        String sellerId=tokenUtil.extractUserId(accessToken);

        if (!doesProductExist(productId)) {
            throw new RuntimeException(PRODUCT_NOT_FOUND);
        }

        if(!doesSellerOwnProduct(productId,sellerId))
        {
            throw new RuntimeException(String.format("Product with ID %s does not belong to Seller with ID %s", productId, sellerId));
        }

        return inventoryRepository.findById(productId).map(product -> {
            // Update the fields using the DTO
            product.setName(updatedProductDTO.getName());
            product.setSellerId(sellerId);
            product.setPrice(updatedProductDTO.getPrice());
            product.setStock(updatedProductDTO.getStock());

            // Save the updated product and return the DTO
            Product updatedProduct = inventoryRepository.save(product);
            logger.info("Product updated successfully - Product ID:{}",productId);
            return updatedProduct.toDTO();
        }).orElseThrow(() -> new RuntimeException(PRODUCT_NOT_FOUND));
    }

    // Delete product by id
    public void deleteProduct(String productId,String accessToken)
    {
        String sellerId=tokenUtil.extractUserId(accessToken);

        if (!doesProductExist(productId))
        {
            throw new RuntimeException(PRODUCT_NOT_FOUND);
        }

        if(!doesSellerOwnProduct(productId,sellerId))
        {
            throw new RuntimeException(String.format("Product with ID %s does not belong to Seller with ID %s", productId, sellerId));
        }

        logger.info("Product deleted successfully - Product ID:{}",productId);
        inventoryRepository.deleteById(productId);
        // Delete product in Product Service
        try {
            inventoryInterServiceClient.deleteProductInProductService(productId, accessToken);
        } catch (Exception e) {
            logger.error("Failed to delete product in Product Service: {}", e.getMessage());
            throw new RuntimeException("Product deleted from Inventory but failed to delete in Product Service");
        }
    }

    public double getTotalBill(List<CartProductDTO> cartProductsDto)
    {
        //get all the product ids in cart product dto
        List<String> productIds = cartProductsDto.stream()
                .map(CartProductDTO::getProductId)
                .collect(Collectors.toList());
        //use the product ids to fetch only the necessary products from db
        List<Product> cartProducts= inventoryRepository.getCartProducts(productIds);

        //map through cart products and for each cart product map through products returned from db and match with the correct product to calcuate bill
        return cartProductsDto.stream()
                .mapToDouble(dto -> {
                    // Find matching Product by ID
                    Product product = cartProducts.stream()
                            .filter(p -> p.getProductId().equals(dto.getProductId()))
                            .findFirst()
                            .orElse(null);

                    // If product is found, multiply price by quantity
                    return (product != null) ? product.getPrice() * dto.getQuantity() : 0.0;
                })
                .sum();
    }

    public boolean doesProductExist(String productId)
    {
        return inventoryRepository.findById(productId).isPresent();
    }

    public boolean isInStock(String productId, int quantity)
    {
        return inventoryRepository.isInStock(productId,quantity);
    }

    public boolean doesSellerOwnProduct(String productId,String sellerId)
    {
        return inventoryRepository.findById(productId).get().getSellerId().equals(sellerId);
    }

    public Optional<Product> getProduct(String productId)
    {
        return inventoryRepository.findById(productId);
    }

    public List<String> getProductIds(String sellerId)
    {
        return inventoryRepository.getProductIds(sellerId);
    }

    public List<Product> getCartProducts(List<String> productIds)
    {
        List<Product> products=inventoryRepository.getCartProducts(productIds);
        return products;
    }

    public Boolean isCartValid(List<CartProductDTO> cartProducts)
    {
        for (CartProductDTO item : cartProducts)
        {
            boolean inStock = inventoryRepository.isInStock(item.getProductId(), item.getQuantity());
            if (!inStock)
            {
                return false;
            }
        }
        //if all items are in stock then reduce stock placed in order
        reduceStock(cartProducts);
        return true;
    }

    private void reduceStock(List<CartProductDTO> cartProducts)
    {
        for(CartProductDTO cartProduct : cartProducts)
        {
            inventoryRepository.reduceStock(cartProduct.getProductId(),cartProduct.getQuantity());
        }
    }
}

