package com.ecommerce.ProductService.Controller;

import com.ecommerce.ProductService.DTO.ProductDTO;
import com.ecommerce.ProductService.Entity.Product;
import com.ecommerce.ProductService.Service.ProductService;
import com.ecommerce.ProductService.Utility.TokenUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/product")
@RequiredArgsConstructor
public class ProductController {


    private final ProductService productService;

    private final TokenUtil tokenUtil;

    private static final Logger logger = LoggerFactory.getLogger(ProductController.class);

    @PreAuthorize("hasRole('SELLER')")
    @PostMapping("/createProduct")
    public ResponseEntity<String> createProduct(@RequestHeader("Authorization") String authorizationHeader,@RequestBody @Valid ProductDTO productDTO)
    {
        String accessToken = authorizationHeader.substring(7);
        return productService.createProduct(productDTO,accessToken);
    }

    @PreAuthorize("hasRole('SELLER')")
    @PutMapping("/updateProduct")
    public ResponseEntity<String> updateProduct(@RequestHeader("Authorization") String authorizationHeader,@RequestBody @Valid ProductDTO productDTO)
    {
        String accessToken = authorizationHeader.substring(7);
        return productService.updateProduct(productDTO,accessToken);
    }


    @PreAuthorize("hasRole('SELLER')")
    @GetMapping("/getAllProductsBySellerId")
    public ResponseEntity<List<Product>> getAllProductsBySellerId(@RequestHeader("Authorization") String authorizationHeader)
    {
        logger.info("GET /getAllProductsBySellerId");
        String accessToken = authorizationHeader.substring(7);
        String sellerId=tokenUtil.extractUserId(accessToken); //done here to have sellerId as redis key
        return ResponseEntity.ok(productService.getAllProductsBySellerId(sellerId,accessToken));
    }


    @GetMapping("/getProductByProductId/{productId}")
    public ResponseEntity<Optional<Product>> getProductByProductId(@PathVariable("productId")String productId)
    {
        logger.info("GET /getProductByProductId Product ID {}",productId);
        return ResponseEntity.ok(productService.getProductByProductId(productId));
    }

    @GetMapping("/getAllProducts")
    public ResponseEntity<List<Product>> getAllProducts()
    {
        logger.info("GET /getAllProducts");
        return ResponseEntity.ok(productService.getAllProducts());
    }

    @PreAuthorize("hasRole('SELLER')")
    @PostMapping("/uploadImage/{productId}")
    public ResponseEntity<?> uploadImage(@RequestHeader("Authorization") String authorizationHeader,@PathVariable String productId,@RequestParam("image") MultipartFile[] imageFiles)
    {
        String accessToken = authorizationHeader.substring(7);
        try {
            List<String> imageUrls = productService.uploadProductImages(accessToken,productId,imageFiles);
            return ResponseEntity.ok(imageUrls);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to upload image: " + e.getMessage());
        }
    }

    @PreAuthorize("hasRole('SELLER')")
    @DeleteMapping("/deleteImages/{productId}")
    public ResponseEntity<?> deleteImages(@RequestHeader("Authorization") String authorizationHeader,
                                          @PathVariable String productId,
                                          @RequestBody List<String> imageUrls) {
        String accessToken = authorizationHeader.substring(7);
        try {
            List<String> deleted = productService.deleteProductImages(accessToken, productId, imageUrls);
            return ResponseEntity.ok("Deleted images: " + deleted);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to delete images: " + e.getMessage());
        }
    }


}
