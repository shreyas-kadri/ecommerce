package com.example.OrderService.Utility;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.Base64;

@Service
public class TokenUtil {

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

}
