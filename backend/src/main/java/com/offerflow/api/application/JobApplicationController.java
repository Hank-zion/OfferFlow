package com.offerflow.api.application;

import com.offerflow.api.application.dto.ApplicationRequest;
import com.offerflow.api.application.dto.ApplicationResponse;
import com.offerflow.api.user.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/applications")
public class JobApplicationController {
    private final JobApplicationService service;
    public JobApplicationController(JobApplicationService service) { this.service = service; }
    @GetMapping public Page<ApplicationResponse> list(@AuthenticationPrincipal CurrentUser user, @RequestParam(required = false) String keyword, @RequestParam(required = false) ApplicationStatus status, @PageableDefault(size = 10, sort = "updatedAt") Pageable pageable) { return service.list(user.user().getId(), keyword, status, pageable).map(ApplicationResponse::from); }
    @PostMapping public ResponseEntity<ApplicationResponse> create(@AuthenticationPrincipal CurrentUser user, @Valid @RequestBody ApplicationRequest request) { return ResponseEntity.status(HttpStatus.CREATED).body(ApplicationResponse.from(service.create(user.user().getId(), request))); }
    @GetMapping("/{id}") public ApplicationResponse get(@AuthenticationPrincipal CurrentUser user, @PathVariable Long id) { return ApplicationResponse.from(service.get(user.user().getId(), id)); }
    @PutMapping("/{id}") public ApplicationResponse update(@AuthenticationPrincipal CurrentUser user, @PathVariable Long id, @Valid @RequestBody ApplicationRequest request) { return ApplicationResponse.from(service.update(user.user().getId(), id, request)); }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@AuthenticationPrincipal CurrentUser user, @PathVariable Long id) { service.delete(user.user().getId(), id); }
}
