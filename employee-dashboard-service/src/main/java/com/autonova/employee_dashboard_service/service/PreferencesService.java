package com.autonova.employee_dashboard_service.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.autonova.employee_dashboard_service.dto.PreferencesRequest;
import com.autonova.employee_dashboard_service.dto.PreferencesResponse;
import com.autonova.employee_dashboard_service.entity.EmployeePreferences;
import com.autonova.employee_dashboard_service.repository.EmployeePreferencesRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PreferencesService {

    private final EmployeePreferencesRepository preferencesRepository;

    @Transactional(readOnly = true)
    public PreferencesResponse getPreferences(Long employeeId) {
        EmployeePreferences preferences = preferencesRepository.findById(employeeId)
                .orElseGet(() -> createDefaultPreferences(employeeId));

        return PreferencesResponse.builder()
                .employeeId(preferences.getEmployeeId())
                .defaultView(preferences.getDefaultView().name().toLowerCase())
                .theme(preferences.getTheme().name().toLowerCase())
                .build();
    }

    @Transactional
    public PreferencesResponse updatePreferences(Long employeeId, PreferencesRequest request) {
        EmployeePreferences preferences = preferencesRepository.findById(employeeId)
                .orElse(EmployeePreferences.builder()
                        .employeeId(employeeId)
                        .build());

        preferences.setDefaultView(EmployeePreferences.ViewType.valueOf(request.getDefaultView().toUpperCase()));
        preferences.setTheme(EmployeePreferences.Theme.valueOf(request.getTheme().toUpperCase()));

        EmployeePreferences saved = preferencesRepository.save(preferences);

        return PreferencesResponse.builder()
                .employeeId(saved.getEmployeeId())
                .defaultView(saved.getDefaultView().name().toLowerCase())
                .theme(saved.getTheme().name().toLowerCase())
                .build();
    }

    private EmployeePreferences createDefaultPreferences(Long employeeId) {
        EmployeePreferences defaultPreferences = EmployeePreferences.builder()
                .employeeId(employeeId)
                .defaultView(EmployeePreferences.ViewType.OPERATIONAL)
                .theme(EmployeePreferences.Theme.LIGHT)
                .build();

        return preferencesRepository.save(defaultPreferences);
    }
}
