package com.offerflow.api.dashboard;

import com.offerflow.api.application.ApplicationStatus;
import com.offerflow.api.application.JobApplicationRepository;
import com.offerflow.api.user.CurrentUser;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final JobApplicationRepository applications;
    public DashboardController(JobApplicationRepository applications) { this.applications = applications; }
    @GetMapping
    public Map<String, Object> summary(@AuthenticationPrincipal CurrentUser user) {
        Long userId = user.user().getId();
        long total = applications.countByUserId(userId);
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (ApplicationStatus status : ApplicationStatus.values()) byStatus.put(status.name(), applications.countByUserIdAndStatus(userId, status));
        long activeApplications = total - byStatus.get(ApplicationStatus.SAVED.name());
        long offers = byStatus.get(ApplicationStatus.OFFER.name());
        double conversionRate = activeApplications == 0 ? 0 : Math.round((offers * 1000.0 / activeApplications)) / 10.0;
        return Map.of("total", total, "byStatus", byStatus, "offerConversionRate", conversionRate);
    }
}
