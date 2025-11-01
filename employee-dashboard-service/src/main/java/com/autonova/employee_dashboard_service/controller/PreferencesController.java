package com.autonova.employee_dashboard_service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.autonova.employee_dashboard_service.dto.PreferencesRequest;
import com.autonova.employee_dashboard_service.dto.PreferencesResponse;
import com.autonova.employee_dashboard_service.service.PreferencesService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/dashboard/preferences")
@RequiredArgsConstructor
public class PreferencesController {

    private final PreferencesService preferencesService;

    @GetMapping
    public ResponseEntity<PreferencesResponse> getPreferences(Authentication authentication) {
        Long employeeId = extractEmployeeId(authentication);
        PreferencesResponse response = preferencesService.getPreferences(employeeId);
        return ResponseEntity.ok(response);
    }

    @PutMapping
    public ResponseEntity<PreferencesResponse> updatePreferences(
            Authentication authentication,
            @RequestBody PreferencesRequest request) {
        Long employeeId = extractEmployeeId(authentication);
        PreferencesResponse response = preferencesService.updatePreferences(employeeId, request);
        return ResponseEntity.ok(response);
    }

    private Long extractEmployeeId(Authentication authentication) {
        // Extract employee ID from authentication token
        // This is a placeholder - implement based on your auth service structure
        String username = authentication.getName();
        // You might need to parse this from JWT claims or fetch from auth service
        return Long.parseLong(username); // Adjust this based on your auth implementation
    }
}
