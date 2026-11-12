package com.offerflow.api.application;

import com.offerflow.api.application.dto.ApplicationRequest;
import com.offerflow.api.common.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
public class JobApplicationService {
    private final JobApplicationRepository repository;
    public JobApplicationService(JobApplicationRepository repository) { this.repository = repository; }
    public Page<JobApplication> list(Long userId, String keyword, ApplicationStatus status, Pageable pageable) {
        Specification<JobApplication> spec = (root, query, builder) -> builder.equal(root.get("userId"), userId);
        if (status != null) spec = spec.and((root, query, builder) -> builder.equal(root.get("status"), status));
        if (keyword != null && !keyword.isBlank()) {
            String pattern = "%" + keyword.trim().toLowerCase() + "%";
            spec = spec.and((root, query, builder) -> builder.or(builder.like(builder.lower(root.get("company")), pattern), builder.like(builder.lower(root.get("position")), pattern)));
        }
        return repository.findAll(spec, pageable);
    }
    public JobApplication get(Long userId, Long id) { return repository.findById(id).filter(item -> item.getUserId().equals(userId)).orElseThrow(() -> new NotFoundException("Application not found")); }
    public JobApplication create(Long userId, ApplicationRequest request) { validateSalary(request); return repository.save(new JobApplication(userId, request.company(), request.position(), request.city(), request.source(), request.status(), request.appliedAt(), request.salaryMin(), request.salaryMax(), request.note())); }
    public JobApplication update(Long userId, Long id, ApplicationRequest request) { validateSalary(request); JobApplication item = get(userId, id); item.update(request.company(), request.position(), request.city(), request.source(), request.status(), request.appliedAt(), request.salaryMin(), request.salaryMax(), request.note()); return repository.save(item); }
    public void delete(Long userId, Long id) { repository.delete(get(userId, id)); }
    private void validateSalary(ApplicationRequest request) { if (request.salaryMin() != null && request.salaryMax() != null && request.salaryMin() > request.salaryMax()) throw new IllegalArgumentException("Minimum salary cannot be greater than maximum salary"); }
}
