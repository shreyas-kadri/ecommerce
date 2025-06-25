package com.ecommerce.InventoryService.Service;

import com.ecommerce.InventoryService.DTO.CartProductDTO;
import com.ecommerce.InventoryService.DTO.ProductDTO;
import com.ecommerce.InventoryService.Entity.Product;
import com.ecommerce.InventoryService.Repository.InventoryRepository;
import com.ecommerce.InventoryService.Utility.TokenUtil;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InventoryServiceTest {

    @InjectMocks
    private InventoryService inventoryService;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private TokenUtil tokenUtil;

    @Mock
    private InventoryInterServiceClient inventoryInterServiceClient;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        inventoryService = new InventoryService(inventoryRepository, tokenUtil, inventoryInterServiceClient);
    }

    @Test
    void testCreateProduct_success() {
        ProductDTO productDTO = new ProductDTO("Test Product", 100.0, 10);
        Product savedProduct = Product.builder()
                .productId("123")
                .name("Test Product")
                .price(100.0)
                .stock(10)
                .sellerId("seller1")
                .build();

        when(tokenUtil.extractUserId("token")).thenReturn("seller1");
        when(inventoryRepository.save(any(Product.class))).thenReturn(savedProduct);

        ProductDTO result = inventoryService.createProduct(productDTO, "token");

        assertEquals("Test Product", result.getName());
        assertEquals(100.0, result.getPrice());
        assertEquals(10, result.getStock());
    }

    @Test
    void testGetProductById_success() {
        Product product = Product.builder()
                .productId("p1")
                .name("Sample")
                .price(99.9)
                .stock(5)
                .sellerId("seller1")
                .build();

        when(tokenUtil.extractUserId("token")).thenReturn("seller1");
        when(inventoryRepository.findById("p1")).thenReturn(Optional.of(product));
        when(inventoryRepository.findProductBySellerId("p1", "seller1")).thenReturn(Optional.of(product));

        ProductDTO dto = inventoryService.getProductById("p1", "token");

        assertEquals("Sample", dto.getName());
    }

    @Test
    void testDeleteProduct_callsDeleteProductInProductService() {
        Product product = Product.builder()
                .productId("p1")
                .sellerId("seller1")
                .build();

        when(tokenUtil.extractUserId("token")).thenReturn("seller1");
        when(inventoryRepository.findById("p1")).thenReturn(Optional.of(product));

        // Simulate delete
        doNothing().when(inventoryRepository).deleteById("p1");

        // Simulate external service call
        doNothing().when(inventoryInterServiceClient).deleteProductInProductService("p1", "token");

        inventoryService.deleteProduct("p1", "token");

        verify(inventoryRepository).deleteById("p1");
        verify(inventoryInterServiceClient).deleteProductInProductService("p1", "token");
    }

    @Test
    void testIsCartValid_allInStock_shouldReturnTrueAndReduceStock() {
        List<CartProductDTO> cart = List.of(
                new CartProductDTO("p1", 2),
                new CartProductDTO("p2", 3)
        );

        when(inventoryRepository.isInStock("p1", 2)).thenReturn(true);
        when(inventoryRepository.isInStock("p2", 3)).thenReturn(true);

        Boolean result = inventoryService.isCartValid(cart);

        assertTrue(result);
        verify(inventoryRepository).reduceStock("p1", 2);
        verify(inventoryRepository).reduceStock("p2", 3);
    }

    @Test
    void testIsCartValid_itemOutOfStock_shouldReturnFalse() {
        List<CartProductDTO> cart = List.of(
                new CartProductDTO("p1", 2),
                new CartProductDTO("p2", 3)
        );

        when(inventoryRepository.isInStock("p1", 2)).thenReturn(true);
        when(inventoryRepository.isInStock("p2", 3)).thenReturn(false);

        Boolean result = inventoryService.isCartValid(cart);

        assertFalse(result);
        verify(inventoryRepository, never()).reduceStock(anyString(), anyInt());
    }
}
