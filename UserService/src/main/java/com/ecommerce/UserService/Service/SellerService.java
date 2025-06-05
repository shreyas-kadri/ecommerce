package com.ecommerce.UserService.Service;

import com.ecommerce.UserService.DTO.UserUpdateDTO;
import com.ecommerce.UserService.Entity.Customer;
import com.ecommerce.UserService.Entity.Seller;
import com.ecommerce.UserService.Repository.SellerRepository;
import com.ecommerce.UserService.Utility.TokenUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SellerService {

    private final SellerRepository sellerRepository;

    private final TokenUtil tokenUtil;

    private static final Logger logger = LoggerFactory.getLogger(SellerService.class);

    public void saveSeller(Seller seller)
    {
        sellerRepository.save(seller);
        logger.info("User saved to database successfully");
    }

    public void updateSeller(String userId, UserUpdateDTO userUpdateDTO)
    {
        sellerRepository.update(userId,userUpdateDTO.getFname(),userUpdateDTO.getLname(),userUpdateDTO.getEmail(),userUpdateDTO.getPhone(),userUpdateDTO.getAddress());
        logger.info("User details updated in database successfully");

    }

    public ResponseEntity<Optional<Seller>> getSellerById(String accessToken)
    {
        String userId=tokenUtil.extractUserId(accessToken);
        String role=tokenUtil.extractUserRole(accessToken);
        Optional<Seller> seller=sellerRepository.findById(userId);
        logger.info("User details fetched successfully - User ID {}",userId);
        return ResponseEntity.ok(seller);
    }

    public ResponseEntity<List<Seller>> getAllSellers()
    {
        List<Seller> sellers= sellerRepository.findAll();
        logger.info("All user details fetched successfully");
        return ResponseEntity.ok(sellers);
    }

}
