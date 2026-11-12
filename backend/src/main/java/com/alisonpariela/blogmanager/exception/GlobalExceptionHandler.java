package com.alisonpariela.blogmanager.exception;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import lombok.extern.slf4j.Slf4j;
import java.util.Map;
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    private ResponseEntity<Map<String,Object>> error(int status, String message) {
        return ResponseEntity.status(status).body(Map.of("status", status, "message", message));
    }
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<?> missing(EntityNotFoundException e) { return error(404, e.getMessage()); }
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<?> forbidden(AccessDeniedException e) { return error(403, "You cannot access this resource"); }
    @ExceptionHandler({AuthenticationException.class, io.jsonwebtoken.JwtException.class})
    public ResponseEntity<?> unauthorized(Exception e) { return error(401, "Invalid credentials"); }
    @ExceptionHandler({IllegalArgumentException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<?> invalid(Exception e) { return error(400, "Invalid request. Check your input."); }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> validation(MethodArgumentNotValidException e) {
        return error(400, String.join("; ", e.getBindingResult().getFieldErrors().stream().map(f -> f.getField() + ": " + f.getDefaultMessage()).toList()));
    }
    @ExceptionHandler({RestClientException.class, IllegalStateException.class})
    public ResponseEntity<?> unavailable(Exception e) { log.warn("Assistant service unavailable: {}", e.getClass().getSimpleName()); return error(503, "AI service unavailable. Please try again later."); }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> unexpected(Exception e) { log.error("Unexpected request failure", e); return error(500, "Something went wrong. Please try again."); }
}
