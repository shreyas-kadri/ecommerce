package com.ecommerce.CartService.Controller;

import com.ecommerce.CartService.DTO.CartSummaryDTO;
import com.ecommerce.CartService.DTO.ProductDTO;
import com.ecommerce.CartService.Service.CartService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
@RequestMapping("/cart")
public class CartController {

    private final CartService cartService;

    private static final Logger logger = LoggerFactory.getLogger(CartController.class);

    @PreAuthorize("hasRole('CUSTOMER')")
    @PostMapping("/addToCart")
    public ResponseEntity<String> addToCart(@RequestHeader("Authorization") String authorizationHeader,@RequestBody ProductDTO productDTO)
    {
        logger.info("POST /addToCart");
        String accessToken = authorizationHeader.substring(7);
        return cartService.addToCart(accessToken,productDTO);
    }

    @PreAuthorize("hasRole('CUSTOMER')")
    @DeleteMapping("/deleteFromCart/{productId}")
    public ResponseEntity<String> deleteFromCart(@RequestHeader("Authorization") String authorizationHeader,@PathVariable String productId)
    {
        logger.info("DELETE /deleteFromCart - Product ID {}",productId);
        String accessToken = authorizationHeader.substring(7);
        return cartService.deleteFromCart(accessToken,productId);
    }

    @PreAuthorize("hasRole('CUSTOMER')")
    @DeleteMapping("/clearCart")
    public ResponseEntity<String> clearCart(@RequestHeader("Authorization") String authorizationHeader)
    {
        logger.info("DELETE /clearCart");
        String accessToken = authorizationHeader.substring(7);
        return cartService.clearCart(accessToken);

    }

    @PreAuthorize("hasRole('CUSTOMER')")
    @GetMapping("/getCartSummary")
    public ResponseEntity<CartSummaryDTO> getCartSummary(@RequestHeader("Authorization") String authorizationHeader)
    {
        logger.info("GET /getCartSummary");
        String accessToken = authorizationHeader.substring(7);
        return cartService.getCartSummary(accessToken);
    }

}
