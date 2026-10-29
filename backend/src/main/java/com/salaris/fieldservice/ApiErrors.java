package com.salaris.fieldservice;

import java.util.Map;
import org.springframework.http.*;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
class ApiErrors {
  @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
  ResponseEntity<Map<String,String>> expected(org.springframework.web.server.ResponseStatusException e) {
    return ResponseEntity.status(e.getStatusCode()).body(Map.of("error", e.getReason()==null?"This record is unavailable or the request is invalid.":e.getReason()));
  }
  @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
  ResponseEntity<Map<String,String>> validation(org.springframework.web.bind.MethodArgumentNotValidException e) {
    String message=e.getBindingResult().getFieldErrors().stream().map(f->f.getField()+": "+f.getDefaultMessage()).findFirst().orElse("Check the supplied fields.");
    return ResponseEntity.badRequest().body(Map.of("error",message));
  }
  @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
  ResponseEntity<Map<String,String>> tooLarge(){return ResponseEntity.status(413).body(Map.of("error","Photo is too large. Choose a JPG or PNG up to 5 MB."));}
  @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
  ResponseEntity<Map<String,String>> staleWrite() {
    return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error","Job changed. Refresh and try again."));
  }
}
