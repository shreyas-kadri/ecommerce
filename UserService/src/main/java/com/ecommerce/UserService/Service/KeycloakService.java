package com.ecommerce.UserService.Service;

import com.ecommerce.UserService.DTO.LoginDTO;
import com.ecommerce.UserService.DTO.UserDTO;
import com.ecommerce.UserService.DTO.UserUpdateDTO;
import com.ecommerce.UserService.Entity.Customer;
import com.ecommerce.UserService.Entity.Seller;
import com.ecommerce.UserService.Utility.TokenUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.keycloak.representations.idm.ClientRepresentation;

import java.util.*;


@Service
@RequiredArgsConstructor
public class KeycloakService {

    @Value("${keycloak.auth-server-url}")
    private String keycloakUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.client-id}")
    private String clientId;

    @Value("${keycloak.client-secret}")
    private String clientSecret;

    //Keycloak admin instance bean
    private final Keycloak keycloak;

    private final CustomerService customerService;

    private final SellerService sellerService;

    private final TokenUtil tokenUtil;

    private static final Logger logger = LoggerFactory.getLogger(KeycloakService.class);

    public String registerUser(UserDTO userDTO) {

        RealmResource realmResource = keycloak.realm(realm);

        // Create user representation
        UserRepresentation user = new UserRepresentation();
        user.setUsername(userDTO.getUsername());
        user.setFirstName(userDTO.getFname());
        user.setLastName(userDTO.getLname());
        user.setEmail(userDTO.getEmail());
        user.setEnabled(true);

        // Set password
        CredentialRepresentation password = new CredentialRepresentation();
        password.setTemporary(false);
        password.setType(CredentialRepresentation.PASSWORD);
        password.setValue(userDTO.getPassword());
        user.setCredentials(Collections.singletonList(password));

        // Create user
        Response response = realmResource.users().create(user);
        if (response.getStatus() == 201) {
            String userId = response.getLocation().getPath().replaceAll(".*/([^/]+)$", "$1");
            // Assign the client role 'USER' to the user
            assignClientRoleToUser(userId, clientId, userDTO.getRole());
            this.saveUser(userId,userDTO,userDTO.getRole());
            logger.info("User registered successfully");
            return "User Registered Successfully!";
        } else {
            logger.error("Failed to register user");
            return "Failed to register user: " + response.getStatus();
        }
    }

    private void assignClientRoleToUser(String userId, String clientId, String roleName) {
        RealmResource realmResource = keycloak.realm(realm);

        try {
            // Fetch the client by its clientId
            ClientRepresentation client = realmResource.clients().findByClientId(clientId).get(0);
            // Get the client role by its name
            RoleRepresentation role = realmResource.clients().get(client.getId()).roles().get(roleName).toRepresentation();
            // Assign the client role to the user
            realmResource.users().get(userId).roles().clientLevel(client.getId()).add(Collections.singletonList(role));
            logger.info("Assigned '{}' client role to user with ID {}", roleName, userId);
        } catch (Exception e) {
            logger.error("Error assigning client role '{}' to user with ID {}: {}", roleName, userId, e.getMessage());
        }
    }

    public ResponseEntity<Map<String, Object>> authenticateUser(LoginDTO loginDTO) {
        try {
            // Authenticate user with Keycloak
            Keycloak keycloak = KeycloakBuilder.builder()
                    .serverUrl(keycloakUrl)
                    .realm(realm)
                    .clientId(clientId)
                    .clientSecret(clientSecret)
                    .username(loginDTO.getUsername())
                    .password(loginDTO.getPassword())
                    .grantType("password")
                    .build();

            // Retrieve the access and refresh tokens
            AccessTokenResponse tokenResponse = keycloak.tokenManager().getAccessToken();

            // Prepare the response map
            Map<String, Object> response = new HashMap<>();
            response.put("message", "User authenticated successfully!");
            response.put("access_token", tokenResponse.getToken());
            response.put("refresh_token", tokenResponse.getRefreshToken());
            response.put("expires_in", tokenResponse.getExpiresIn()); // Optional: Expiry time
            logger.info("User authenticated successfully");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            // Return an error response if authentication fails
            logger.error("User not authenticated invalid username or password");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid username or password"));
        }
    }

    public ResponseEntity<String> logoutUser(String accessToken) {
        try {
            HttpClient client = HttpClients.createDefault();
            HttpPost post = new HttpPost(keycloakUrl + "/realms/" + realm + "/protocol/openid-connect/logout");

            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("client_id", clientId));
            params.add(new BasicNameValuePair("client_secret", clientSecret));
            params.add(new BasicNameValuePair("token", accessToken));

            post.setEntity(new UrlEncodedFormEntity(params));
            HttpResponse response = client.execute(post);

            if (response.getStatusLine().getStatusCode() == 200) {
                logger.info("User logged out successfully");
                return ResponseEntity.ok("User logged out successfully!");
            } else {
                logger.error("Failed to log out user. Status: {}", response.getStatusLine().getStatusCode());
                return ResponseEntity.status(response.getStatusLine().getStatusCode()).body("Failed to log out user.");
            }
        } catch (Exception e) {
            logger.error("Error during logout: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error during logout.");
        }
    }

    public ResponseEntity<String> updateUser(String accessToken, UserUpdateDTO userUpdateDTO)
    {
        String userId=tokenUtil.extractUserId(accessToken);
        String role=tokenUtil.extractUserRole(accessToken);
        try {
            RealmResource realmResource = keycloak.realm(realm);
            // Fetch the existing user
            UserRepresentation user = realmResource.users().get(userId).toRepresentation();

            if (user == null) {
                logger.error("User with ID {} not found", userId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
            }

            // Update user details
            user.setUsername(userUpdateDTO.getUsername());
            user.setFirstName(userUpdateDTO.getFname());
            user.setLastName(userUpdateDTO.getLname());
            user.setEmail(userUpdateDTO.getEmail());

            // Update the user in Keycloak
            realmResource.users().get(userId).update(user);

            //update user in database
            this.updateUser(userId,userUpdateDTO,role);
            logger.info("User with ID {} updated successfully", userId);
            return ResponseEntity.ok("User updated successfully!");

        } catch (Exception e) {
            logger.error("Error updating user with ID {}: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error updating user: " + e.getMessage());
        }
    }

    public ResponseEntity<Map<String, Object>> refreshAccessToken(String refreshToken) {
        try {
            // Create an HTTP client
            HttpClient client = HttpClients.createDefault();
            HttpPost post = new HttpPost(keycloakUrl + "/realms/" + realm + "/protocol/openid-connect/token");

            // Add the request parameters
            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("client_id", clientId));
            params.add(new BasicNameValuePair("client_secret", clientSecret));
            params.add(new BasicNameValuePair("grant_type", "refresh_token"));
            params.add(new BasicNameValuePair("refresh_token", refreshToken));

            post.setEntity(new UrlEncodedFormEntity(params));

            // Execute the request
            HttpResponse response = client.execute(post);
            int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode == 200) {
                // Parse the response
                AccessTokenResponse tokenResponse = new ObjectMapper().readValue(response.getEntity().getContent(), AccessTokenResponse.class);

                Map<String, Object> responseBody = new HashMap<>();
                responseBody.put("access_token", tokenResponse.getToken());
                responseBody.put("refresh_token", tokenResponse.getRefreshToken());
                responseBody.put("expires_in", tokenResponse.getExpiresIn());

                logger.info("Access token refreshed successfully.");
                return ResponseEntity.ok(responseBody);
            } else {
                logger.error("Failed to refresh access token. Status: {}", statusCode);
                return ResponseEntity.status(statusCode).body(Map.of("error", "Failed to refresh access token."));
            }
        } catch (Exception e) {
            logger.error("Error while refreshing access token: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Error while refreshing access token."));
        }
    }

    public void saveUser(String userId,UserDTO userDTO, String role)
    {
        switch (role)
        {
            case "CUSTOMER":
                Customer customer= Customer.builder()
                                .customerId(userId)
                                .username(userDTO.getUsername())
                                .fname(userDTO.getFname())
                                .lname(userDTO.getLname())
                                .email(userDTO.getEmail())
                                .phone(userDTO.getPhone())
                                .address(userDTO.getAddress())
                                .build();
                customerService.saveCustomer(customer);
                break;

            case "SELLER":
                Seller seller= Seller.builder()
                        .sellerId(userId)
                        .username(userDTO.getUsername())
                        .fname(userDTO.getFname())
                        .lname(userDTO.getLname())
                        .email(userDTO.getEmail())
                        .phone(userDTO.getPhone())
                        .address(userDTO.getAddress())
                        .build();
                sellerService.saveSeller(seller);
                break;

            default:
                throw new IllegalArgumentException("Invalid role: " + role);
        }
    }

    public void updateUser(String userId, UserUpdateDTO userUpdateDTO, String role)
    {
        switch (role)
        {
            case "CUSTOMER":
                customerService.updateCustomer(userId, userUpdateDTO);
                break;

            case "SELLER":
                sellerService.updateSeller(userId, userUpdateDTO);
                break;

            default:
                throw new IllegalArgumentException("Invalid role: " + role);
        }
    }

}
