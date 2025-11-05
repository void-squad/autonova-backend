package com.autonova.employee_dashboard_service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
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
    public ResponseEntity<PreferencesResponse> getPreferences(
            @RequestHeader(value = "X-Employee-Id", defaultValue = "1") Long employeeId) {
        PreferencesResponse response = preferencesService.getPreferences(employeeId);
        return ResponseEntity.ok(response);
    }

    @PutMapping
    public ResponseEntity<PreferencesResponse> updatePreferences(
            @RequestHeader(value = "X-Employee-Id", defaultValue = "1") Long employeeId,
            @RequestBody PreferencesRequest request) {
        PreferencesResponse response = preferencesService.updatePreferences(employeeId, request);
        return ResponseEntity.ok(response);
    }

    // TODO: Re-enable authentication
    // private Long extractEmployeeId(Authentication authentication) {
    //     String username = authentication.getName();
    //     return Long.parseLong(username);
    // }
}
