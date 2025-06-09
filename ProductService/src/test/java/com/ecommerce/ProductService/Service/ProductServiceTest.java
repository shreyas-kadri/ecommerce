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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private Cloudinary cloudinary;

    @Mock
    private TokenUtil tokenUtil;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private ProductService productService;

    private String accessToken;
    private String productId;
    private Product product;
    private ProductDTO productDTO;
    private String sellerId;

    @BeforeEach
    void setUp() {
        productService = new ProductService(
                productRepository,
                tokenUtil,
                restTemplate,
                cloudinary
        );
        accessToken = "dummyAccessToken";
        productId = UUID.randomUUID().toString();
        sellerId=UUID.randomUUID().toString();

        product = Product.builder()
                .productId(productId)
                .name("ProductName")
                .description("ProductDescription")
                .price(100.0)
                .category(Category.ELECTRONICS)
                .details(Map.of("color", "black", "warranty", "2 years"))
                .build();

        productDTO = ProductDTO.builder()
                .productId(productId)
                .description("ProductDescription")
                .category(Category.ELECTRONICS)
                .details(Map.of("color", "black", "warranty", "2 years"))
                .build();

    }

    @Test
    void createProduct_ValidProduct_SavesSuccessfully() {
        when(productRepository.save(any(Product.class))).thenReturn(product);
        when(tokenUtil.extractUserId(accessToken)).thenReturn(sellerId);
        ResponseEntity<Boolean> responseEntity = ResponseEntity.ok(true);
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(Boolean.class)
        )).thenReturn(responseEntity);

        when(restTemplate.exchange(
                contains("/doesSellerOwnProduct?productId=" + productId + "&sellerId=" + sellerId),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(Boolean.class)
        )).thenReturn(responseEntity);
        NameAndPriceDTO expectedDto = new NameAndPriceDTO("Product A", 99.99);
        ResponseEntity<NameAndPriceDTO> namePriceDTO = new ResponseEntity<>(expectedDto, HttpStatus.OK);

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(NameAndPriceDTO.class)
        )).thenReturn(namePriceDTO);
        ResponseEntity<String> response = productService.createProduct(productDTO,accessToken);
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    void getProduct_ProductExists_ReturnsProductDTO() {
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        Optional<Product> result = productService.getProductByProductId(productId);

        assertTrue(result.isPresent());
        assertEquals(productId, result.get().getProductId());
        assertEquals("ProductName", result.get().getName());
        assertEquals("ProductDescription",result.get().getDescription());
        assertEquals(100,result.get().getPrice());
        assertEquals(Category.ELECTRONICS, result.get().getCategory());
        assertEquals("black", result.get().getDetails().get("color"));
    }

    @Test
    void getProduct_ProductNotFound_ReturnsEmpty() {
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        Optional<Product> result = productService.getProductByProductId(productId);

        assertTrue(result.isEmpty());
    }

    @Test
    void updateProduct_ProductExists_UpdatesSuccessfully()
    {
        when(productRepository.save(any(Product.class))).thenReturn(product);
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(tokenUtil.extractUserId(accessToken)).thenReturn(sellerId);
        ResponseEntity<Boolean> responseEntity = ResponseEntity.ok(true);
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(Boolean.class)
        )).thenReturn(responseEntity);

        ResponseEntity<String> response = productService.updateProduct(productDTO,accessToken);
        assertEquals("Product updated successfully", response.getBody());
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    void deleteProduct_ProductExists_DeletesSuccessfully() {
        doNothing().when(productRepository).deleteById(productId);
        // Call the method (void return)
        productService.deleteProduct(productId);

        verify(productRepository, times(1)).deleteById(productId);
    }


    @Test
    void getAllProducts_ReturnsListOfProducts() {
        List<Product> products = List.of(product);
        when(productRepository.findAll()).thenReturn(products);

        List<Product> result = productService.getAllProducts();

        assertFalse(result.isEmpty());
        assertEquals(productId, result.get(0).getProductId());
        assertEquals("ProductName", result.get(0).getName());
        assertEquals("ProductDescription",result.get(0).getDescription());
        assertEquals(100,result.get(0).getPrice());
        assertEquals(Category.ELECTRONICS, result.get(0).getCategory());
        assertEquals("black", result.get(0).getDetails().get("color"));
    }
}
