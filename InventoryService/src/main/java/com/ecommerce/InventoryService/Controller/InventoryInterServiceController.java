package com.ecommerce.InventoryService.Controller;

import com.ecommerce.InventoryService.DTO.CartProductDTO;
import com.ecommerce.InventoryService.DTO.NameAndPriceDTO;
import com.ecommerce.InventoryService.Entity.Product;
import com.ecommerce.InventoryService.Service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

// all apis only required for inter-service communication

@RestController
@RequestMapping("/inventoryService")
@RequiredArgsConstructor
public class InventoryInterServiceController {

    private final InventoryService inventoryService;

    private static final Logger logger = LoggerFactory.getLogger(InventoryInterServiceController.class);

    @GetMapping("/doesProductExist")
    public boolean doesProductExist(@RequestParam("productId") String productId)
    {
        logger.info("GET /doesProductExist Product ID {}",productId);
        return inventoryService.doesProductExist(productId);
    }

    @GetMapping("/isInStock")
    public boolean isInStock(@RequestParam("productId") String productId,@RequestParam("quantity") int quantity)
    {
        logger.info("GET /isInStock Product ID {}",productId);
        return inventoryService.isInStock(productId,quantity);
    }

    @GetMapping("/doesSellerOwnProduct")
    public boolean doesSellerOwnProduct(@RequestParam("productId") String productId,@RequestParam("sellerId") String sellerId)
    {
        logger.info("GET /doesSellerOwnProduct Product ID {} Seller ID {}",productId,sellerId);
        return inventoryService.doesSellerOwnProduct(productId,sellerId);
    }

    @PostMapping("/getTotalBill")
    public double getTotalBill(@RequestBody List<CartProductDTO> cartProducts)
    {
        logger.info("POST /getTotalBill");
        return inventoryService.getTotalBill(cartProducts);
    }

    @GetMapping("/fetchNameAndPrice")
    public NameAndPriceDTO fetchNameAndPrice(@RequestParam("productId") String productId)
    {
        logger.info("GET /fetchNameAndPrice Product ID {}",productId);
        Optional<Product> product= inventoryService.getProduct(productId);
        return new NameAndPriceDTO(product.get().getName(),product.get().getPrice());
    }

    @PostMapping("/getCartProducts")
    public List<Product> fetchProductPrices(@RequestBody List<String> productIds)
    {
        logger.info("POST /getCartProducts");
        return inventoryService.getCartProducts(productIds);

    }

    //used by order service to check if the products in cart are in stock
    @PostMapping("/isCartValid")
    public Boolean isCartValid(@RequestBody List<CartProductDTO> cartProducts)
    {
        logger.info("POST /isCartValid");
        return inventoryService.isCartValid(cartProducts);
    }

    @GetMapping("/getProductIds/{sellerId}")
    public List<String> getProductIds(@PathVariable String sellerId)
    {
        logger.info("GET /getProductIds Seller ID {}",sellerId);
        return inventoryService.getProductIds(sellerId);
    }
}
