package com.ecommerce.UserService.Controller;

import com.ecommerce.UserService.DTO.LoginDTO;
import com.ecommerce.UserService.DTO.UserDTO;
import com.ecommerce.UserService.DTO.UserUpdateDTO;
import com.ecommerce.UserService.Entity.Customer;
import com.ecommerce.UserService.Entity.Seller;
import com.ecommerce.UserService.Service.CustomerService;
import com.ecommerce.UserService.Service.KeycloakService;
import com.ecommerce.UserService.Service.SellerService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final KeycloakService keycloakService;

    private final CustomerService customerService;

    private final SellerService sellerService;

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    // User Registration Endpoint
    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@RequestBody UserDTO userDTO) {
        logger.info("POST /register");
        String message = keycloakService.registerUser(userDTO);
        return ResponseEntity.ok(message);
    }

    // User Login Endpoint
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> loginUser(@RequestBody LoginDTO loginDTO) {
        logger.info("POST /authenticate");
        return keycloakService.authenticateUser(loginDTO);
    }

    //user logout endpiunt
    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestHeader("Authorization") String authorizationHeader) {
        String accessToken = authorizationHeader.substring(7); // Remove "Bearer " prefix
        return keycloakService.logoutUser(accessToken);
    }

    //user update endpoint
    @PutMapping("/updateDetails")
    public ResponseEntity<String> updateUser(@RequestHeader("Authorization") String authorizationHeader, @RequestBody UserUpdateDTO userUpdateDTO)
    {
        logger.info("PUT /updateDetails");
        String accessToken = authorizationHeader.substring(7);
        return keycloakService.updateUser(accessToken,userUpdateDTO);
    }

    //fetch customer detail by id
    @PreAuthorize("hasRole('CUSTOMER')")
    @GetMapping("/getCustomerById")
    public ResponseEntity<Optional<Customer>> getCustomerById(@RequestHeader("Authorization") String authorizationHeader)
    {
        logger.info("GET /getCustomerById");
        String accessToken = authorizationHeader.substring(7);
        return customerService.getCustomerById(accessToken);
    }

    //fetch seller detail by id
    @PreAuthorize("hasRole('SELLER')")
    @GetMapping("/getSellerById")
    public ResponseEntity<Optional<Seller>> getSellerById(@RequestHeader("Authorization") String authorizationHeader)
    {
        logger.info("GET /getSellerById");
        String accessToken = authorizationHeader.substring(7);
        return sellerService.getSellerById(accessToken);
    }

    //fetch all customers
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/getAllCustomers")
    public ResponseEntity<List<Customer>> getAllCustomers()
    {
        logger.info("GET /getAllCustomers");
        return customerService.getAllCustomers();
    }

    //fetch all sellers
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/getAllSellers")
    public ResponseEntity<List<Seller>> getAllSellers()
    {
        logger.info("GET /getAllSellers");
        return sellerService.getAllSellers();
    }

    //refresh token endpoint to get new access token
    @PostMapping("/refresh-token")
    public ResponseEntity<Map<String, Object>> refreshToken(@RequestParam String refreshToken) {
        return keycloakService.refreshAccessToken(refreshToken);
    }

}
