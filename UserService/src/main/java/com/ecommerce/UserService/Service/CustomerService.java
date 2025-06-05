package com.ecommerce.UserService.Service;

import com.ecommerce.UserService.DTO.UserUpdateDTO;
import com.ecommerce.UserService.Entity.Customer;
import com.ecommerce.UserService.Repository.CustomerRepository;
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
public class CustomerService {

    private final CustomerRepository customerRepository;

    private final TokenUtil tokenUtil;

    private static final Logger logger = LoggerFactory.getLogger(CustomerService.class);

    public void saveCustomer(Customer customer)
    {
        customerRepository.save(customer);
        logger.info("User saved to database successfully");
    }

    public void updateCustomer(String userId, UserUpdateDTO userUpdateDTO)
    {
        customerRepository.update(userId,userUpdateDTO.getFname(),userUpdateDTO.getLname(),userUpdateDTO.getEmail(),userUpdateDTO.getPhone(),userUpdateDTO.getAddress());
        logger.info("User details updated in database successfully");

    }

    public ResponseEntity<Optional<Customer>> getCustomerById(String accessToken)
    {
        String userId=tokenUtil.extractUserId(accessToken);
        String role=tokenUtil.extractUserRole(accessToken);
        Optional<Customer> customer= customerRepository.findById(userId);
        logger.info("User details fetched successfully - User ID {}",userId);
        return ResponseEntity.ok(customer);
    }

    public ResponseEntity<List<Customer>> getAllCustomers()
    {
        List<Customer> customers= customerRepository.findAll();
        logger.info("All user details fetched successfully");
        return ResponseEntity.ok(customers);
    }

}
