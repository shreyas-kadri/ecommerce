package com.ecommerce.InventoryService.Controller;

import com.ecommerce.InventoryService.DTO.ProductDTO;
import com.ecommerce.InventoryService.Service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    private static final Logger logger = LoggerFactory.getLogger(InventoryController.class);

    @PreAuthorize("hasRole('SELLER')")
    @PostMapping("/createProduct")
    public ResponseEntity<ProductDTO> createProduct(@RequestHeader("Authorization") String authorizationHeader,@RequestBody @Valid ProductDTO productDTO) {
        logger.info("POST /createProduct");
        String accessToken = authorizationHeader.substring(7);
        ProductDTO createdProduct = inventoryService.createProduct(productDTO,accessToken);
        return ResponseEntity.ok(createdProduct);
    }

    @PreAuthorize("hasRole('SELLER')")
    @PostMapping("/createMultipleProducts")
    public ResponseEntity<List<ProductDTO>> createMultipleProducts(@RequestHeader("Authorization") String authorizationHeader,@RequestBody List<ProductDTO> productDTOs) {
        logger.info("POST /createMultipleProducts");
        String accessToken = authorizationHeader.substring(7);
        return ResponseEntity.ok(inventoryService.createMultipleProducts(productDTOs,accessToken));
    }

    @PreAuthorize("hasRole('SELLER')")
    @GetMapping("/getAllProductsBySellerId")
    public ResponseEntity<List<ProductDTO>> getAllProducts(@RequestHeader("Authorization") String authorizationHeader) {
        logger.info("GET /getAllProductsBySellerId");
        String accessToken = authorizationHeader.substring(7);
        List<ProductDTO> products = inventoryService.getAllProducts(accessToken);
        return ResponseEntity.ok(products);
    }

    @PreAuthorize("hasRole('SELLER')")
    @GetMapping("/getProductByProductId/{productId}")
    public ResponseEntity<ProductDTO> getProductById(@RequestHeader("Authorization") String authorizationHeader,@PathVariable String productId)
    {
        logger.info("GET /getByProductByProductId - Product ID: {}",productId);
        String accessToken = authorizationHeader.substring(7);
        return ResponseEntity.ok(inventoryService.getProductById(productId,accessToken));
    }

    @PreAuthorize("hasRole('SELLER')")
    @PutMapping("/update/{productId}")
    public ResponseEntity<ProductDTO> updateProduct(@RequestHeader("Authorization") String authorizationHeader,@PathVariable String productId, @RequestBody @Valid ProductDTO productDTO) {
        logger.info("PUT /update - Product ID: {}",productId);
        String accessToken = authorizationHeader.substring(7);
        return ResponseEntity.ok(inventoryService.updateProduct(productId, productDTO,accessToken));
    }

    @PreAuthorize("hasRole('SELLER')")
    @DeleteMapping("/deleteProduct/{productId}")
    public ResponseEntity<String> deleteProduct(@RequestHeader("Authorization") String authorizationHeader,@PathVariable String productId) {
        logger.info("DELETE /deleteProduct - Product ID: {}",productId);
        String accessToken = authorizationHeader.substring(7);
        inventoryService.deleteProduct(productId,accessToken);
        return ResponseEntity.ok("Product deleted successfully");
    }

}
