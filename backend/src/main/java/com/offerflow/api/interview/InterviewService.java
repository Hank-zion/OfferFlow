package com.offerflow.api.interview;

import com.offerflow.api.application.JobApplicationService;
import com.offerflow.api.common.NotFoundException;
import com.offerflow.api.interview.dto.InterviewRequest;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class InterviewService {
    private final InterviewRepository repository; private final JobApplicationService applications;
    public InterviewService(InterviewRepository repository, JobApplicationService applications) { this.repository = repository; this.applications = applications; }
    public List<Interview> list(Long userId, Long applicationId) { applications.get(userId, applicationId); return repository.findByApplicationIdOrderByRoundNumberAsc(applicationId); }
    public Interview create(Long userId, Long applicationId, InterviewRequest request) { applications.get(userId, applicationId); validateUniqueRound(applicationId, request.roundNumber()); return repository.save(new Interview(applicationId, request.roundNumber(), request.interviewedAt(), request.result(), request.questions(), request.reflection())); }
    public Interview update(Long userId, Long applicationId, Long id, InterviewRequest request) { applications.get(userId, applicationId); Interview interview = require(applicationId, id); validateUniqueRound(applicationId, request.roundNumber(), id); interview.update(request.roundNumber(), request.interviewedAt(), request.result(), request.questions(), request.reflection()); return repository.save(interview); }
    public void delete(Long userId, Long applicationId, Long id) { applications.get(userId, applicationId); repository.delete(require(applicationId, id)); }
    private Interview require(Long applicationId, Long id) { return repository.findById(id).filter(item -> item.getApplicationId().equals(applicationId)).orElseThrow(() -> new NotFoundException("Interview record not found")); }
    private void validateUniqueRound(Long applicationId, Integer roundNumber) { if (repository.existsByApplicationIdAndRoundNumber(applicationId, roundNumber)) throw new IllegalArgumentException("Interview round already exists"); }
    private void validateUniqueRound(Long applicationId, Integer roundNumber, Long id) { if (repository.existsByApplicationIdAndRoundNumberAndIdNot(applicationId, roundNumber, id)) throw new IllegalArgumentException("Interview round already exists"); }
}
