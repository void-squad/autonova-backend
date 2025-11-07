package com.autonova.employee_dashboard_service.service;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.autonova.employee_dashboard_service.dto.PreferencesRequest;
import com.autonova.employee_dashboard_service.dto.PreferencesResponse;
import com.autonova.employee_dashboard_service.entity.EmployeePreferences;
import com.autonova.employee_dashboard_service.repository.EmployeePreferencesRepository;

@ExtendWith(MockitoExtension.class)
class PreferencesServiceTest {

    @Mock
    private EmployeePreferencesRepository preferencesRepository;

    @InjectMocks
    private PreferencesService preferencesService;

    private EmployeePreferences testPreferences;
    private Long employeeId;

    @BeforeEach
    void setUp() {
        employeeId = 1L;
        testPreferences = EmployeePreferences.builder()
                .employeeId(employeeId)
                .defaultView(EmployeePreferences.ViewType.OPERATIONAL)
                .theme(EmployeePreferences.Theme.LIGHT)
                .build();
    }

    @Test
    void getPreferences_WhenPreferencesExist_ShouldReturnPreferences() {
        // Arrange
        when(preferencesRepository.findById(employeeId)).thenReturn(Optional.of(testPreferences));

        // Act
        PreferencesResponse response = preferencesService.getPreferences(employeeId);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getEmployeeId()).isEqualTo(employeeId);
        assertThat(response.getDefaultView()).isEqualTo("operational");
        assertThat(response.getTheme()).isEqualTo("light");
        verify(preferencesRepository, times(1)).findById(employeeId);
        verify(preferencesRepository, never()).save(any());
    }

    @Test
    void getPreferences_WhenPreferencesDoNotExist_ShouldCreateDefaultPreferences() {
        // Arrange
        when(preferencesRepository.findById(employeeId)).thenReturn(Optional.empty());
        when(preferencesRepository.save(any(EmployeePreferences.class))).thenReturn(testPreferences);

        // Act
        PreferencesResponse response = preferencesService.getPreferences(employeeId);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getEmployeeId()).isEqualTo(employeeId);
        assertThat(response.getDefaultView()).isEqualTo("operational");
        assertThat(response.getTheme()).isEqualTo("light");
        verify(preferencesRepository, times(1)).findById(employeeId);
        verify(preferencesRepository, times(1)).save(any(EmployeePreferences.class));
    }

    @Test
    void updatePreferences_WhenPreferencesExist_ShouldUpdateAndReturnPreferences() {
        // Arrange
        PreferencesRequest request = PreferencesRequest.builder()
                .defaultView("analytical")
                .theme("dark")
                .build();

        EmployeePreferences updatedPreferences = EmployeePreferences.builder()
                .employeeId(employeeId)
                .defaultView(EmployeePreferences.ViewType.ANALYTICAL)
                .theme(EmployeePreferences.Theme.DARK)
                .build();

        when(preferencesRepository.findById(employeeId)).thenReturn(Optional.of(testPreferences));
        when(preferencesRepository.save(any(EmployeePreferences.class))).thenReturn(updatedPreferences);

        // Act
        PreferencesResponse response = preferencesService.updatePreferences(employeeId, request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getEmployeeId()).isEqualTo(employeeId);
        assertThat(response.getDefaultView()).isEqualTo("analytical");
        assertThat(response.getTheme()).isEqualTo("dark");
        verify(preferencesRepository, times(1)).findById(employeeId);
        verify(preferencesRepository, times(1)).save(any(EmployeePreferences.class));
    }

    @Test
    void updatePreferences_WhenPreferencesDoNotExist_ShouldCreateNewPreferences() {
        // Arrange
        PreferencesRequest request = PreferencesRequest.builder()
                .defaultView("analytical")
                .theme("dark")
                .build();

        EmployeePreferences newPreferences = EmployeePreferences.builder()
                .employeeId(employeeId)
                .defaultView(EmployeePreferences.ViewType.ANALYTICAL)
                .theme(EmployeePreferences.Theme.DARK)
                .build();

        when(preferencesRepository.findById(employeeId)).thenReturn(Optional.empty());
        when(preferencesRepository.save(any(EmployeePreferences.class))).thenReturn(newPreferences);

        // Act
        PreferencesResponse response = preferencesService.updatePreferences(employeeId, request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getEmployeeId()).isEqualTo(employeeId);
        assertThat(response.getDefaultView()).isEqualTo("analytical");
        assertThat(response.getTheme()).isEqualTo("dark");
        verify(preferencesRepository, times(1)).findById(employeeId);
        verify(preferencesRepository, times(1)).save(any(EmployeePreferences.class));
    }

    @Test
    void updatePreferences_WithOperationalView_ShouldUpdateCorrectly() {
        // Arrange
        PreferencesRequest request = PreferencesRequest.builder()
                .defaultView("operational")
                .theme("light")
                .build();

        when(preferencesRepository.findById(employeeId)).thenReturn(Optional.of(testPreferences));
        when(preferencesRepository.save(any(EmployeePreferences.class))).thenReturn(testPreferences);

        // Act
        PreferencesResponse response = preferencesService.updatePreferences(employeeId, request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getDefaultView()).isEqualTo("operational");
        assertThat(response.getTheme()).isEqualTo("light");
        verify(preferencesRepository, times(1)).save(any(EmployeePreferences.class));
    }
}
