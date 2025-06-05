package com.ecommerce.UserService.Utility;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Base64;

@Service
public class TokenUtil {

    @Value("${keycloak.client-id}")
    private String clientId;

    public String extractUserId(String accessToken)
    {
        try {
            String[] chunks = accessToken.split("\\.");
            Base64.Decoder decoder = Base64.getUrlDecoder();
            String payload = new String(decoder.decode(chunks[1]));
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(payload);
            String userId=jsonNode.get("sub").asText();
            return userId;
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract userId from token", e);
        }
    }

    public String extractUserRole(String accessToken)
    {
        try{
            String[] chunks = accessToken.split("\\.");
            Base64.Decoder decoder = Base64.getUrlDecoder();
            String payload = new String(decoder.decode(chunks[1]));
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(payload);
            JsonNode resourceAccessNode = jsonNode.get("resource_access");
            if (resourceAccessNode != null && resourceAccessNode.has(clientId)) {
                JsonNode clientNode = resourceAccessNode.get(clientId);
                JsonNode rolesNode = clientNode.get("roles");
                if (rolesNode != null && rolesNode.isArray() && !rolesNode.isEmpty()) {
                    return rolesNode.get(0).asText();
                }
            }

            return null;

        }
        catch(Exception e)
        {
            throw new RuntimeException("Failed to extract role from token",e);
        }
    }

}
