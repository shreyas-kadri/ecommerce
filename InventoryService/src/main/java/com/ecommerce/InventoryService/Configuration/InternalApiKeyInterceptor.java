package com.ecommerce.InventoryService.Configuration;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

//interceptor to check for api-key for inter-service apis only
@Component
@RequiredArgsConstructor
public class InternalApiKeyInterceptor implements HandlerInterceptor {

    @Value("${interservice.api.key}")
    private String interserviceKey;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String apiKey = request.getHeader("Internal-API-Key");

        if (!interserviceKey.equals(apiKey)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access Denied: Invalid API Key");
            return false;
        }
        return true;
    }
}
