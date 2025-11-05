package com.autonova.employee_dashboard_service.controller;

import com.autonova.employee_dashboard_service.dto.PreferencesRequest;
import com.autonova.employee_dashboard_service.dto.PreferencesResponse;
import com.autonova.employee_dashboard_service.service.PreferencesService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PreferencesController.class)
class PreferencesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PreferencesService preferencesService;

    @Test
    @WithMockUser(username = "1")
    void getPreferences_ShouldReturnPreferences() throws Exception {
        PreferencesResponse response = PreferencesResponse.builder()
                .employeeId(1L)
                .defaultView("operational")
                .theme("light")
                .build();

        when(preferencesService.getPreferences(anyLong())).thenReturn(response);

        mockMvc.perform(get("/api/dashboard/preferences"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeId").value(1))
                .andExpect(jsonPath("$.defaultView").value("operational"))
                .andExpect(jsonPath("$.theme").value("light"));
    }

    @Test
    @WithMockUser(username = "1")
    void updatePreferences_ShouldReturnUpdatedPreferences() throws Exception {
        PreferencesResponse response = PreferencesResponse.builder()
                .employeeId(1L)
                .defaultView("analytical")
                .theme("dark")
                .build();

        when(preferencesService.updatePreferences(anyLong(), any(PreferencesRequest.class)))
                .thenReturn(response);

        String requestBody = """
                {
                    "defaultView": "analytical",
                    "theme": "dark"
                }
                """;

        mockMvc.perform(put("/api/dashboard/preferences")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeId").value(1))
                .andExpect(jsonPath("$.defaultView").value("analytical"))
                .andExpect(jsonPath("$.theme").value("dark"));
    }
}
