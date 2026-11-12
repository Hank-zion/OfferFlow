package com.offerflow.api.interview.dto;

import com.offerflow.api.interview.InterviewResult;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record InterviewRequest(@NotNull @Min(1) @Max(20) Integer roundNumber, LocalDateTime interviewedAt,
                               @NotNull InterviewResult result, @Size(max = 10000) String questions,
                               @Size(max = 10000) String reflection) { }
