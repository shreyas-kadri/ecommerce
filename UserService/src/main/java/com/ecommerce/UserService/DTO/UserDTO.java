package com.ecommerce.UserService.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDTO {

    private String fname;
    private String lname;
    private String email;
    private String phone;
    private String username;
    private String password;
    private String address;
    private String role;

}
