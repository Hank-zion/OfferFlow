package com.offerflow.api.interview;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InterviewRepository extends JpaRepository<Interview, Long> {
    List<Interview> findByApplicationIdOrderByRoundNumberAsc(Long applicationId);
    boolean existsByApplicationIdAndRoundNumber(Long applicationId, Integer roundNumber);
    boolean existsByApplicationIdAndRoundNumberAndIdNot(Long applicationId, Integer roundNumber, Long id);
}
