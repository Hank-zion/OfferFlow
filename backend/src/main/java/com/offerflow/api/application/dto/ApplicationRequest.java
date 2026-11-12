package com.offerflow.api.application.dto;

import com.offerflow.api.application.ApplicationStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record ApplicationRequest(
        @NotBlank @Size(max = 120) String company,
        @NotBlank @Size(max = 120) String position,
        @Size(max = 60) String city,
        @Size(max = 80) String source,
        @NotNull ApplicationStatus status,
        LocalDate appliedAt,
        @PositiveOrZero Integer salaryMin,
        @PositiveOrZero Integer salaryMax,
        @Size(max = 2000) String note
) { }
