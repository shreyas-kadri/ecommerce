package com.ecommerce.UserService.Entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@Table(name = "sellers")
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Seller {

    @Id
    private String sellerId;
    private String username;
    private String fname;
    private String lname;
    private String email;
    private String phone;
    private String address;

}
