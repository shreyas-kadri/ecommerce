package com.ecommerce.CartService.Service;
import com.ecommerce.CartService.DTO.CartSummaryDTO;
import com.ecommerce.CartService.DTO.ProductDTO;
import com.ecommerce.CartService.Entities.Cart;
import com.ecommerce.CartService.Entities.CartItem;
import com.ecommerce.CartService.Repository.CartItemRepository;
import com.ecommerce.CartService.Repository.CartRepository;
import com.ecommerce.CartService.Utility.TokenUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private TokenUtil tokenUtil;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private CartService cartService;

    private String accessToken;
    private String userId;
    private String cartId;
    private ProductDTO productDTO;

    @BeforeEach
    void setUp() {
        accessToken = "dummyToken";
        userId = UUID.randomUUID().toString();
        cartId = UUID.randomUUID().toString();
        productDTO = new ProductDTO("prod123", 2);
    }

    @Test
    void addToCart_ProductExistsAndInStock_AddsProduct() {
        when(tokenUtil.extractUserId(accessToken)).thenReturn(userId);
        when(cartRepository.doesCartExist(userId)).thenReturn(false);
        when(restTemplate.exchange(any(String.class), eq(org.springframework.http.HttpMethod.GET), any(), eq(Boolean.class)))
                .thenReturn(ResponseEntity.ok(true));

        ResponseEntity<String> response = cartService.addToCart(accessToken, productDTO);

        assertEquals("Item added to cart", response.getBody());
        verify(cartRepository, times(1)).save(any(Cart.class));
        verify(cartItemRepository, times(1)).save(any(CartItem.class));
    }

    @Test
    void addToCart_ProductDoesNotExist_ReturnsProductNotFound() {
        when(restTemplate.exchange(any(String.class), eq(org.springframework.http.HttpMethod.GET), any(), eq(Boolean.class)))
                .thenReturn(ResponseEntity.ok(false));

        ResponseEntity<String> response = cartService.addToCart(accessToken, productDTO);

        assertEquals("Product not found", response.getBody());
        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    void deleteFromCart_ProductExists_DeletesProduct() {
        when(tokenUtil.extractUserId(accessToken)).thenReturn(userId);
        when(cartRepository.findCartId(userId)).thenReturn(Optional.of(cartId));
        when(cartItemRepository.isProductInCart(cartId, productDTO.getProductId())).thenReturn(true);

        ResponseEntity<String> response = cartService.deleteFromCart(accessToken, productDTO.getProductId());

        assertEquals("Item deleted successfully", response.getBody());
        verify(cartItemRepository, times(1)).deleteProductFromCart(cartId, productDTO.getProductId());
    }

    @Test
    void getCartSummary_CartExists_ReturnsSummary() {
        when(tokenUtil.extractUserId(accessToken)).thenReturn(userId);
        when(cartRepository.findCartId(userId)).thenReturn(Optional.of(cartId));
        when(cartItemRepository.getProductsInCart(cartId)).thenReturn(List.of(productDTO));
        when(restTemplate.exchange(any(String.class), eq(org.springframework.http.HttpMethod.POST), any(), eq(Double.class)))
                .thenReturn(ResponseEntity.ok(100.0));

        ResponseEntity<CartSummaryDTO> response = cartService.getCartSummary(accessToken);

        assertNotNull(response.getBody());
        assertEquals(cartId, response.getBody().getCartId());
        assertEquals(100.0, response.getBody().getTotalBill());
    }

    @Test
    void clearCart_CartExists_ClearsSuccessfully() {
        when(tokenUtil.extractUserId(accessToken)).thenReturn(userId);
        when(cartRepository.findCartId(userId)).thenReturn(Optional.of(cartId));

        ResponseEntity<String> response = cartService.clearCart(accessToken);

        assertEquals("Cart cleared successfully", response.getBody());
        verify(cartItemRepository, times(1)).clearCart(cartId);
    }
}
