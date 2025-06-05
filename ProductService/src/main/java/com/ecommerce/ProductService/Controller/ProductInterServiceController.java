package com.ecommerce.ProductService.Controller;

import com.ecommerce.ProductService.Service.ProductService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.web.bind.annotation.*;

import java.util.List;


// all apis only required for inter-service communication
@RestController
@RequiredArgsConstructor
@RequestMapping("/productService")
public class ProductInterServiceController {

    private final ProductService productService;

    private static final Logger logger = LoggerFactory.getLogger(ProductInterServiceController.class);

    @DeleteMapping("/deleteProductByProductId")
    public void deleteProductByProductId(@RequestParam("productId") String productId)
    {
        logger.info("DELETE /deleteProductByProductId Product ID {}",productId);
        productService.deleteProduct(productId);
    }
}
