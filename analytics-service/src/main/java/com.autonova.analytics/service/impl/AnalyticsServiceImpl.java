package com.autonova.analytics.service.impl;

import com.autonova.analytics.dto.*;
import com.autonova.analytics.entity.Activity;
import com.autonova.analytics.entity.EmployeePerformance;
import com.autonova.analytics.repository.ActivityRepository;
import com.autonova.analytics.repository.EmployeePerformanceRepository;
import com.autonova.analytics.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private final EmployeePerformanceRepository employeePerformanceRepository;
    private final ActivityRepository activityRepository;

    @Value("${services.customer.url}")
    private String customerServiceUrl;

    @Value("${services.appointment.url}")
    private String appointmentServiceUrl;

    @Value("${services.payment.url}")
    private String paymentServiceUrl;

    @Value("${services.project.url}")
    private String projectServiceUrl;

    @Override
    public DashboardAnalyticsDto getDashboardData() {
        // Fetch Stats
        StatsDto stats = fetchStats();

        // Fetch Recent Activity
        List<ActivityDto> recentActivity = fetchRecentActivity();

        // Fetch Top Employees
        List<EmployeePerformanceDto> topEmployees = fetchTopEmployees();

        return new DashboardAnalyticsDto(stats, recentActivity, topEmployees);
    }

    private StatsDto fetchStats() {
        long totalCustomers = 0;
        long activeAppointments = 0;
        double monthlyRevenue = 0.0;
        long activeProjects = 0;

        try {
            totalCustomers = Long.parseLong(fetchFromService(customerServiceUrl + "/api/customers/count"));
        } catch (Exception e) {
            System.err.println("Error fetching customers: " + e.getMessage());
        }

        try {
            activeAppointments = Long.parseLong(fetchFromService(appointmentServiceUrl + "/api/appointments/active/count"));
        } catch (Exception e) {
            System.err.println("Error fetching appointments: " + e.getMessage());
        }

        try {
            monthlyRevenue = Double.parseDouble(fetchFromService(paymentServiceUrl + "/api/payments/month/revenue"));
        } catch (Exception e) {
            System.err.println("Error fetching revenue: " + e.getMessage());
        }

        try {
            activeProjects = Long.parseLong(fetchFromService(projectServiceUrl + "/api/projects/active/count"));
        } catch (Exception e) {
            System.err.println("Error fetching projects: " + e.getMessage());
        }

        return new StatsDto(totalCustomers, activeAppointments, monthlyRevenue, activeProjects);
    }

    private List<ActivityDto> fetchRecentActivity() {
        List<Activity> activities = activityRepository.findRecentActivities();

        return activities.stream()
                .limit(10)
                .map(activity -> new ActivityDto(
                        String.valueOf(activity.getId()),
                        activity.getType(),
                        activity.getMessage(),
                        getTimeAgo(activity.getTimestamp())
                ))
                .collect(Collectors.toList());
    }

    private List<EmployeePerformanceDto> fetchTopEmployees() {
        List<EmployeePerformance> employees = employeePerformanceRepository.findTopPerformers();

        return employees.stream()
                .limit(5)
                .map(emp -> new EmployeePerformanceDto(
                        emp.getEmployeeName(),
                        emp.getTasksCompleted(),
                        emp.getHoursLogged(),
                        emp.getEfficiency()
                ))
                .collect(Collectors.toList());
    }

    private String fetchFromService(String urlString) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                StringBuilder content = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }
                in.close();
                connection.disconnect();

                return content.toString();
            } else {
                throw new RuntimeException("Failed to fetch data from service: " + urlString);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error calling service: " + urlString, e);
        }
    }

    private String getTimeAgo(LocalDateTime timestamp) {
        Duration duration = Duration.between(timestamp, LocalDateTime.now());

        long seconds = duration.getSeconds();
        if (seconds < 60) return seconds + " seconds ago";

        long minutes = seconds / 60;
        if (minutes < 60) return minutes + " minute" + (minutes > 1 ? "s" : "") + " ago";

        long hours = minutes / 60;
        if (hours < 24) return hours + " hour" + (hours > 1 ? "s" : "") + " ago";

        long days = hours / 24;
        return days + " day" + (days > 1 ? "s" : "") + " ago";
    }
}