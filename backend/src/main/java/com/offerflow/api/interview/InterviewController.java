package com.offerflow.api.interview;

import com.offerflow.api.interview.dto.InterviewRequest;
import com.offerflow.api.interview.dto.InterviewResponse;
import com.offerflow.api.user.CurrentUser;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/applications/{applicationId}/interviews")
public class InterviewController {
    private final InterviewService service;
    public InterviewController(InterviewService service) { this.service = service; }
    @GetMapping public List<InterviewResponse> list(@AuthenticationPrincipal CurrentUser user, @PathVariable Long applicationId) { return service.list(user.user().getId(), applicationId).stream().map(InterviewResponse::from).toList(); }
    @PostMapping public ResponseEntity<InterviewResponse> create(@AuthenticationPrincipal CurrentUser user, @PathVariable Long applicationId, @Valid @RequestBody InterviewRequest request) { return ResponseEntity.status(HttpStatus.CREATED).body(InterviewResponse.from(service.create(user.user().getId(), applicationId, request))); }
    @PutMapping("/{id}") public InterviewResponse update(@AuthenticationPrincipal CurrentUser user, @PathVariable Long applicationId, @PathVariable Long id, @Valid @RequestBody InterviewRequest request) { return InterviewResponse.from(service.update(user.user().getId(), applicationId, id, request)); }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@AuthenticationPrincipal CurrentUser user, @PathVariable Long applicationId, @PathVariable Long id) { service.delete(user.user().getId(), applicationId, id); }
}
