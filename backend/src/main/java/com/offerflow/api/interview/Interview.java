package com.offerflow.api.interview;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "interviews")
public class Interview {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "application_id", nullable = false) private Long applicationId;
    @Column(name = "round_number", nullable = false) private Integer roundNumber;
    private LocalDateTime interviewedAt;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private InterviewResult result = InterviewResult.PENDING;
    @Column(columnDefinition = "TEXT") private String questions;
    @Column(columnDefinition = "TEXT") private String reflection;
    @Column(nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(nullable = false) private LocalDateTime updatedAt;
    protected Interview() { }
    public Interview(Long applicationId, Integer roundNumber, LocalDateTime interviewedAt, InterviewResult result, String questions, String reflection) { this.applicationId = applicationId; update(roundNumber, interviewedAt, result, questions, reflection); }
    @PrePersist void created() { createdAt = LocalDateTime.now(); updatedAt = createdAt; }
    @PreUpdate void updated() { updatedAt = LocalDateTime.now(); }
    public void update(Integer roundNumber, LocalDateTime interviewedAt, InterviewResult result, String questions, String reflection) { this.roundNumber = roundNumber; this.interviewedAt = interviewedAt; this.result = result; this.questions = questions; this.reflection = reflection; }
    public Long getId() { return id; } public Long getApplicationId() { return applicationId; } public Integer getRoundNumber() { return roundNumber; }
    public LocalDateTime getInterviewedAt() { return interviewedAt; } public InterviewResult getResult() { return result; }
    public String getQuestions() { return questions; } public String getReflection() { return reflection; } public LocalDateTime getCreatedAt() { return createdAt; } public LocalDateTime getUpdatedAt() { return updatedAt; }
}
