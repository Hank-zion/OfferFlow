package com.offerflow.api.application.dto;

import com.offerflow.api.application.ApplicationStatus;
import com.offerflow.api.application.JobApplication;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ApplicationResponse(Long id, String company, String position, String city, String source,
                                  ApplicationStatus status, LocalDate appliedAt, Integer salaryMin,
                                  Integer salaryMax, String note, LocalDateTime createdAt, LocalDateTime updatedAt) {
    public static ApplicationResponse from(JobApplication application) {
        return new ApplicationResponse(application.getId(), application.getCompany(), application.getPosition(),
                application.getCity(), application.getSource(), application.getStatus(), application.getAppliedAt(),
                application.getSalaryMin(), application.getSalaryMax(), application.getNote(),
                application.getCreatedAt(), application.getUpdatedAt());
    }
}
