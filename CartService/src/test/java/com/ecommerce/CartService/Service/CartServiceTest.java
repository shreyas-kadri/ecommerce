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

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CartServiceTest {

    @InjectMocks
    private CartService cartService;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private TokenUtil tokenUtil;

    @Mock
    private CartInterServiceClient cartInterServiceClient;

    private ProductDTO sampleProduct;

    @BeforeEach
    void setup() {
        sampleProduct = new ProductDTO();
        sampleProduct.setProductId("prod-1");
        sampleProduct.setQuantity(2);
    }

    @Test
    void addToCart_WhenProductInvalid_ShouldReturnNotFoundMessage() {
        when(cartInterServiceClient.doesProductExists(anyString(), anyString())).thenReturn(false);

        ResponseEntity<String> response = cartService.addToCart("token", sampleProduct);

        assertEquals("Product not found", response.getBody());
        verify(cartInterServiceClient, times(1)).doesProductExists(anyString(), anyString());
    }

    @Test
    void addToCart_WhenOutOfStock_ShouldReturnOutOfStockMessage() {
        when(cartInterServiceClient.doesProductExists(anyString(), anyString())).thenReturn(true);
        when(cartInterServiceClient.isInStock(any(), anyString())).thenReturn(false);

        ResponseEntity<String> response = cartService.addToCart("token", sampleProduct);

        assertEquals("Product is not in stock", response.getBody());
    }

    @Test
    void addToCart_WhenNewCart_ShouldCreateCartAndAddItem() {
        when(cartInterServiceClient.doesProductExists(anyString(), anyString())).thenReturn(true);
        when(cartInterServiceClient.isInStock(any(), anyString())).thenReturn(true);
        when(tokenUtil.extractUserId(anyString())).thenReturn("user-1");
        when(cartRepository.doesCartExist("user-1")).thenReturn(false);

        ResponseEntity<String> response = cartService.addToCart("token", sampleProduct);

        assertEquals("Item added to cart", response.getBody());
        verify(cartRepository).save(any(Cart.class));
        verify(cartItemRepository).save(any(CartItem.class));
    }

    @Test
    void addToCart_WhenCartExistsAndProductExists_ShouldUpdateQuantity() {
        Cart existingCart = new Cart("cart-1", "user-1");

        when(cartInterServiceClient.doesProductExists(anyString(), anyString())).thenReturn(true);
        when(cartInterServiceClient.isInStock(any(), anyString())).thenReturn(true);
        when(tokenUtil.extractUserId(anyString())).thenReturn("user-1");
        when(cartRepository.doesCartExist("user-1")).thenReturn(true);
        when(cartRepository.findCartId("user-1")).thenReturn(Optional.of("cart-1"));
        when(cartRepository.findById("cart-1")).thenReturn(Optional.of(existingCart));
        when(cartItemRepository.isProductInCart("cart-1", "prod-1")).thenReturn(true);

        ResponseEntity<String> response = cartService.addToCart("token", sampleProduct);

        assertEquals("Item added to cart", response.getBody());
        verify(cartItemRepository).update("cart-1", "prod-1", 2);
    }

    @Test
    void deleteFromCart_WhenProductNotInCart_ShouldReturnMessage() {
        when(tokenUtil.extractUserId(anyString())).thenReturn("user-1");
        when(cartRepository.findCartId("user-1")).thenReturn(Optional.of("cart-1"));
        when(cartItemRepository.isProductInCart("cart-1", "prod-1")).thenReturn(false);

        ResponseEntity<String> response = cartService.deleteFromCart("token", "prod-1");

        assertTrue(response.getBody().contains("Cannot delete"));
        verify(cartItemRepository, never()).deleteProductFromCart(anyString(), anyString());
    }

    @Test
    void deleteFromCart_WhenCartMissing_ShouldReturnMessage() {
        when(tokenUtil.extractUserId(anyString())).thenReturn("user-1");
        when(cartRepository.findCartId("user-1")).thenReturn(Optional.empty());

        ResponseEntity<String> response = cartService.deleteFromCart("token", "prod-1");

        assertTrue(response.getBody().contains("cart does not exist"));
    }

    @Test
    void deleteFromCart_WhenProductExists_ShouldDeleteSuccessfully() {
        when(tokenUtil.extractUserId(anyString())).thenReturn("user-1");
        when(cartRepository.findCartId("user-1")).thenReturn(Optional.of("cart-1"));
        when(cartItemRepository.isProductInCart("cart-1", "prod-1")).thenReturn(true);

        ResponseEntity<String> response = cartService.deleteFromCart("token", "prod-1");

        assertEquals("Item deleted successfully", response.getBody());
        verify(cartItemRepository).deleteProductFromCart("cart-1", "prod-1");
    }

    @Test
    void getCartSummary_WhenCartMissing_ShouldReturnNotFound() {
        when(tokenUtil.extractUserId(anyString())).thenReturn("user-1");
        when(cartRepository.findCartId("user-1")).thenReturn(Optional.empty());

        ResponseEntity<CartSummaryDTO> response = cartService.getCartSummary("token");

        assertEquals(404, response.getStatusCodeValue());
    }

    @Test
    void getCartSummary_WhenCartEmpty_ShouldReturnNotFound() {
        when(tokenUtil.extractUserId(anyString())).thenReturn("user-1");
        when(cartRepository.findCartId("user-1")).thenReturn(Optional.of("cart-1"));
        when(cartItemRepository.getProductsInCart("cart-1")).thenReturn(Collections.emptyList());

        ResponseEntity<CartSummaryDTO> response = cartService.getCartSummary("token");

        assertEquals(404, response.getStatusCodeValue());
    }

    @Test
    void getCartSummary_WhenCartHasProducts_ShouldReturnSummary() {
        List<ProductDTO> products = new ArrayList<>();
        ProductDTO product = new ProductDTO();
        product.setProductId("prod-1");
        product.setQuantity(2);
        products.add(product);

        when(tokenUtil.extractUserId(anyString())).thenReturn("user-1");
        when(cartRepository.findCartId("user-1")).thenReturn(Optional.of("cart-1"));
        when(cartItemRepository.getProductsInCart("cart-1")).thenReturn(products);
        when(cartInterServiceClient.getTotalBill(products, "token")).thenReturn(500.0);

        ResponseEntity<CartSummaryDTO> response = cartService.getCartSummary("token");

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(500.0, response.getBody().getTotalBill());
    }

    @Test
    void clearCart_WhenCartMissing_ShouldReturnMessage() {
        when(tokenUtil.extractUserId(anyString())).thenReturn("user-1");
        when(cartRepository.findCartId("user-1")).thenReturn(Optional.empty());

        ResponseEntity<String> response = cartService.clearCart("token");

        assertEquals("User has no cart cannot clear cart", response.getBody());
    }

    @Test
    void clearCart_WhenCartExists_ShouldClearSuccessfully() {
        when(tokenUtil.extractUserId(anyString())).thenReturn("user-1");
        when(cartRepository.findCartId("user-1")).thenReturn(Optional.of("cart-1"));

        ResponseEntity<String> response = cartService.clearCart("token");

        assertEquals("Cart cleared successfully", response.getBody());
        verify(cartItemRepository).clearCart("cart-1");
    }
}
