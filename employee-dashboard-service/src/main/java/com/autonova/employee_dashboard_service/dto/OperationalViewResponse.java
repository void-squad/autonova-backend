package com.autonova.employee_dashboard_service.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperationalViewResponse {
    private ActiveTimerData activeTimer;
    private List<AppointmentData> todaysAppointments;
    private List<WorkQueueItem> workQueue;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActiveTimerData {
        private Long timerId;
        private Long employeeId;
        private String jobId;
        private String status;
        private String startTime;
        private Long elapsedSeconds;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AppointmentData {
        private Long appointmentId;
        private String customerName;
        private String appointmentTime;
        private String serviceType;
        private String status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkQueueItem {
        private Long jobId;
        private String jobType;
        private String priority;
        private String status;
        private String assignedTo;
        private String deadline;
    }
}
