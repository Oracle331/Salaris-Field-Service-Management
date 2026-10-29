package com.salaris.fieldservice;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/jobs")
class JobController {
  @Autowired
  private JobService jobService;

  @PostMapping
  public ResponseEntity<Job> createJob(@Valid @RequestBody Job job, Authentication authentication) {
    return ResponseEntity.status(HttpStatus.CREATED).body(jobService.saveJob(job, authentication));
  }

  @PostMapping("/{id}/prep")
  public ResponseEntity<JobPrepResult> prepareJob(@PathVariable Long id, Authentication authentication) {
    return ResponseEntity.ok(jobService.prepareJob(id, authentication));
  }
}
