package com.ecommerce.ProductService.Service;

import com.cloudinary.Cloudinary;
import com.ecommerce.ProductService.DTO.NameAndPriceDTO;
import com.ecommerce.ProductService.DTO.ProductDTO;
import com.ecommerce.ProductService.Entity.Product;
import com.ecommerce.ProductService.Enum.Category;
import com.ecommerce.ProductService.Repository.ProductRepository;
import com.ecommerce.ProductService.Utility.TokenUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private TokenUtil tokenUtil;

    @Mock
    private Cloudinary cloudinary;

    @Mock
    private ProductInterServiceClient productInterServiceClient;

    @InjectMocks
    private ProductService productService;

    private String accessToken;
    private String productId;
    private String sellerId;
    private Product product;
    private ProductDTO productDTO;

    @BeforeEach
    void setUp() {
        accessToken = "dummyAccessToken";
        productId = UUID.randomUUID().toString();
        sellerId = UUID.randomUUID().toString();

        product = Product.builder()
                .productId(productId)
                .name("Product A")
                .description("Description")
                .price(99.99)
                .category(Category.ELECTRONICS)
                .details(Map.of("color", "black", "warranty", "2 years"))
                .build();

        productDTO = ProductDTO.builder()
                .productId(productId)
                .description("Description")
                .category(Category.ELECTRONICS)
                .details(Map.of("color", "black", "warranty", "2 years"))
                .build();
    }

    @Test
    void createProduct_ValidProduct_Success() {
        when(tokenUtil.extractUserId(accessToken)).thenReturn(sellerId);
        when(productInterServiceClient.doesProductExist(productId, accessToken)).thenReturn(true);
        when(productInterServiceClient.doesSellerOwnProduct(productId, sellerId, accessToken)).thenReturn(true);
        when(productInterServiceClient.fetchNameAndPrice(productId, accessToken)).thenReturn(new NameAndPriceDTO("Product A", 99.99));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        ResponseEntity<String> response = productService.createProduct(productDTO, accessToken);

        assertEquals("Product created successfully", response.getBody());
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void updateProduct_ValidProduct_UpdatesSuccessfully() {
        when(tokenUtil.extractUserId(accessToken)).thenReturn(sellerId);
        when(productInterServiceClient.doesProductExist(productId, accessToken)).thenReturn(true);
        when(productInterServiceClient.doesSellerOwnProduct(productId, sellerId, accessToken)).thenReturn(true);
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        ResponseEntity<String> response = productService.updateProduct(productDTO, accessToken);

        assertEquals("Product updated successfully", response.getBody());
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void getProductById_ExistingProduct_ReturnsProduct() {
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        Optional<Product> result = productService.getProductByProductId(productId);

        assertTrue(result.isPresent());
        assertEquals(productId, result.get().getProductId());
    }

    @Test
    void getProductById_ProductNotFound_ReturnsEmpty() {
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        Optional<Product> result = productService.getProductByProductId(productId);

        assertTrue(result.isEmpty());
    }

    @Test
    void getAllProducts_ReturnsProductList() {
        when(productRepository.findAll()).thenReturn(List.of(product));

        List<Product> products = productService.getAllProducts();

        assertEquals(1, products.size());
        assertEquals(productId, products.get(0).getProductId());
    }

    @Test
    void deleteProduct_ProductExists_DeletesSuccessfully() {
        doNothing().when(productRepository).deleteById(productId);

        productService.deleteProduct(productId);

        verify(productRepository).deleteById(productId);
    }
}
