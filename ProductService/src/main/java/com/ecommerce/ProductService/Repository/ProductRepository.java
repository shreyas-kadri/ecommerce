package com.ecommerce.ProductService.Repository;

import com.ecommerce.ProductService.Entity.Product;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface ProductRepository extends MongoRepository<Product,String> {

    @Query("{ 'sellerId' : ?0 }")
    List<Product> findAllProductsBySellerId(String sellerId);

    @Query("{ '_id' : { $in: ?0 } }")
    List<Product> findProductsByProductIds(List<String> productIds);
}
