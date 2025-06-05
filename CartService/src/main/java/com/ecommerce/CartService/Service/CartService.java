package com.ecommerce.CartService.Service;

import com.ecommerce.CartService.DTO.CartSummaryDTO;
import com.ecommerce.CartService.DTO.ProductDTO;
import com.ecommerce.CartService.Entities.Cart;
import com.ecommerce.CartService.Entities.CartItem;
import com.ecommerce.CartService.Repository.CartItemRepository;
import com.ecommerce.CartService.Repository.CartRepository;
import com.ecommerce.CartService.Utility.TokenUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;

    private final CartItemRepository cartItemRepository;

    private final TokenUtil tokenUtil;

    private final RestTemplate restTemplate;

    @Value("${inventory.service.url}")
    private String inventoryServiceUrl;

    @Value("${interservice.api.key}")
    private String interServiceKey;

    private static final Logger logger = LoggerFactory.getLogger(CartService.class);

    public ResponseEntity<String> addToCart(String accessToken, ProductDTO productDTO)
    {
        //check if it is a valid product which exists in product db
        if(!doesProductExists(productDTO.getProductId(),accessToken))
        {
            logger.info("Product with ID {} does not exist",productDTO.getProductId());
            return ResponseEntity.ok("Product not found");
        }

        //check if product is in stock
        if(!isInStock(productDTO,accessToken))
        {
            logger.info("Product with ID {} is not in stock",productDTO.getProductId());
            return ResponseEntity.ok("Product is not in stock");
        }
        String userId=tokenUtil.extractUserId(accessToken);

        //check if cart already exists for that particular user
        if(cartRepository.doesCartExist(userId))
        {
            logger.info("Cart for User ID {} exists",userId);
            Optional<String> cartId=cartRepository.findCartId(userId);
            Optional<Cart> cart=cartRepository.findById(cartId.get());
            //check if product already exists in cart
            if(cartItemRepository.isProductInCart(cartId.get(),productDTO.getProductId()))
            {
                logger.info("Product with ID {} already exists in cart,updating quantity..",productDTO.getProductId());
                //if it exists then just update the quantity
                cartItemRepository.update(cartId.get(),productDTO.getProductId(),productDTO.getQuantity());
            }
            else
            {
                logger.info("Product with ID {} does not exist in cart,adding it to cart",productDTO.getProductId());
                //if it doesnt exist then add new cart item
                addCartItem(productDTO, cart.get());
            }
        }
        else
        {
            logger.info("Cart for User ID {} does not exist,creating new cart..",userId);
            //if cart does not exist then create a new cart and add cart item
            String cartId= UUID.randomUUID().toString();
            Cart cart=new Cart(cartId,userId);
            cartRepository.save(cart);
            addCartItem(productDTO,cart);
        }
        logger.info("Item added successfully to cart");
        return ResponseEntity.ok("Item added to cart");
    }

    public void addCartItem(ProductDTO productDTO,Cart cart)
    {
        String cartItemId=UUID.randomUUID().toString();
        CartItem cartItems=new CartItem(cartItemId,productDTO.getProductId(),productDTO.getQuantity(),cart);
        cartItemRepository.save(cartItems);
    }

    public ResponseEntity<String> deleteFromCart(String accessToken, String productId)
    {
        String userId=tokenUtil.extractUserId(accessToken);
        Optional<String> cartId=cartRepository.findCartId(userId);

        //check if cart is present
        if(cartId.isPresent())
        {
            //check if product is present in cart
            if(cartItemRepository.isProductInCart(cartId.get(),productId))
            {
                //product exists,delete from cart
                cartItemRepository.deleteProductFromCart(cartId.get(), productId);
            }
            else
            {
                //product does not exist in cart,cannot perform delete
                logger.info("Product ID {} does not exist in cart for User ID {}",productId,userId);
                return ResponseEntity.ok(String.format("Cannot delete,Product with ID %s does not exist in cart for User ID %s",productId,cartId));
            }
        }
        else
        {
            //cart does not exist cannot perform delete
            logger.info("User ID {} does not have a cart,cannot perform delete",userId);
            return ResponseEntity.ok(String.format("User ID %s cart does not exist,cannot perform delete",userId));
        }
        logger.info("Product with ID {} deleted successfully from User ID {} cart",productId,cartId.get());
        return ResponseEntity.ok("Item deleted successfully");
    }


    private boolean doesProductExists(String productId,String accessToken)
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

    private boolean isInStock(ProductDTO productDTO,String accessToken)
    {
        //call inventory service to verify whether product is in stock
        try {
            String url = inventoryServiceUrl + "/isInStock?productId=" + productDTO.getProductId() + "&quantity=" + productDTO.getQuantity();
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

    public ResponseEntity<CartSummaryDTO> getCartSummary(String accessToken)
    {
        String userId=tokenUtil.extractUserId(accessToken);
        Optional<String> cartId=cartRepository.findCartId(userId);
        //check if cart is empty
        if(cartId.isEmpty())
        {
            logger.info("No cart found for User ID {}",userId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }

        //check if there are no products in cart
        List<ProductDTO> cartProducts=cartItemRepository.getProductsInCart(cartId.get());

        if(cartProducts.isEmpty())
        {
            logger.info("No products found in cart for Cart of User ID {}",userId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }

        double totalBill=getTotalBill(cartProducts,accessToken);

        return ResponseEntity.ok(new CartSummaryDTO(cartId.get(),cartProducts,totalBill));

    }

    private double getTotalBill(List<ProductDTO> cartProducts, String accessToken) {
        try {
            //call product service to get total bill
            String url = inventoryServiceUrl + "/getTotalBill";

            HttpHeaders headers = new HttpHeaders();
            headers.set("Internal-API-Key",interServiceKey);
            headers.setBearerAuth(accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<List<ProductDTO>> entity = new HttpEntity<>(cartProducts, headers);

            ResponseEntity<Double> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, Double.class
            );
            return response.getBody() != null ? response.getBody() : 0.0;
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch total bill: " + e.getMessage());
        }
    }

    public ResponseEntity<String> clearCart(String accessToken)
    {
        String userId=tokenUtil.extractUserId(accessToken);
        Optional<String> cartId=cartRepository.findCartId(userId);
        if(cartId.isEmpty())
        {
            logger.info("User with ID {} has no cart",userId);
            return ResponseEntity.ok("User has no cart cannot clear cart");
        }

        cartItemRepository.clearCart(cartId.get());
        logger.info("User ID {} cart cleared successfully",userId);
        return ResponseEntity.ok("Cart cleared successfully");

    }

}
