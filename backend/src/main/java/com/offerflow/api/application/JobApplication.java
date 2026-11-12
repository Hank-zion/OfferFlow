package com.offerflow.api.application;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "applications")
public class JobApplication {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(nullable = false, length = 120)
    private String company;
    @Column(nullable = false, length = 120)
    private String position;
    @Column(length = 60)
    private String city;
    @Column(length = 80)
    private String source;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private ApplicationStatus status = ApplicationStatus.APPLIED;
    private LocalDate appliedAt;
    private Integer salaryMin;
    private Integer salaryMax;
    @Column(length = 2000)
    private String note;
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    protected JobApplication() { }

    public JobApplication(Long userId, String company, String position, String city, String source,
                          ApplicationStatus status, LocalDate appliedAt, Integer salaryMin,
                          Integer salaryMax, String note) {
        this.userId = userId;
        update(company, position, city, source, status, appliedAt, salaryMin, salaryMax, note);
    }

    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); updatedAt = createdAt; }
    @PreUpdate void onUpdate() { updatedAt = LocalDateTime.now(); }

    public void update(String company, String position, String city, String source, ApplicationStatus status,
                       LocalDate appliedAt, Integer salaryMin, Integer salaryMax, String note) {
        this.company = company; this.position = position; this.city = city; this.source = source;
        this.status = status; this.appliedAt = appliedAt; this.salaryMin = salaryMin;
        this.salaryMax = salaryMax; this.note = note;
    }
    public Long getId() { return id; } public Long getUserId() { return userId; }
    public String getCompany() { return company; } public String getPosition() { return position; }
    public String getCity() { return city; } public String getSource() { return source; }
    public ApplicationStatus getStatus() { return status; } public LocalDate getAppliedAt() { return appliedAt; }
    public Integer getSalaryMin() { return salaryMin; } public Integer getSalaryMax() { return salaryMax; }
    public String getNote() { return note; } public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
