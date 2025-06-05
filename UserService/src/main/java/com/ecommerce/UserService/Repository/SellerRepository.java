package com.ecommerce.UserService.Repository;

import com.ecommerce.UserService.Entity.Seller;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SellerRepository extends JpaRepository<Seller,String> {

    @Modifying
    @Transactional
    @Query(value = "UPDATE sellers " +
            "SET fname = COALESCE(:fname, fname), " +
            "lname = COALESCE(:lname, lname), " +
            "email = COALESCE(:email, email), " +
            "phone = COALESCE(:phone, phone), " +
            "address = COALESCE(:address, address) " +
            "WHERE seller_id = :sellerId",
            nativeQuery = true)
    void update(@Param("sellerId") String sellerId,
                @Param("fname") String fname,
                @Param("lname") String lname,
                @Param("email") String email,
                @Param("phone") String phone,
                @Param("address") String address);

}
