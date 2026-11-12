package com.offerflow.api.interview.dto;

import com.offerflow.api.interview.Interview;
import com.offerflow.api.interview.InterviewResult;
import java.time.LocalDateTime;

public record InterviewResponse(Long id, Long applicationId, Integer roundNumber, LocalDateTime interviewedAt,
                                InterviewResult result, String questions, String reflection, LocalDateTime createdAt, LocalDateTime updatedAt) {
    public static InterviewResponse from(Interview interview) { return new InterviewResponse(interview.getId(), interview.getApplicationId(), interview.getRoundNumber(), interview.getInterviewedAt(), interview.getResult(), interview.getQuestions(), interview.getReflection(), interview.getCreatedAt(), interview.getUpdatedAt()); }
}
