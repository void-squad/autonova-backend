package com.automobileservice.time_logging_service.client;

import com.automobileservice.time_logging_service.client.dto.UserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "auth-service")
public interface AuthServiceClient {
    
    @GetMapping("/api/users/{id}")
    UserResponse getUserById(@PathVariable("id") String id);
    
    @GetMapping("/api/users")
    List<UserResponse> getUsersByRole(@RequestParam("role") String role);
}
