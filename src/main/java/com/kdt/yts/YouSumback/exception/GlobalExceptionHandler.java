package com.kdt.yts.YouSumback.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        log.error("Validation error occurred: {}", ex.getMessage());
        
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.put(error.getField(), error.getDefaultMessage()));
                
        Map<String, Object> response = new HashMap<>();
        response.put("code", 400);
        response.put("message", "잘못된 요청 데이터입니다.");
        response.put("errors", fieldErrors);
        response.put("data", Map.of());
        
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleUserAlreadyExistsException(UserAlreadyExistsException ex) {
        log.error("User already exists: {}", ex.getMessage());
        
        Map<String, Object> response = new HashMap<>();
        response.put("code", 409);
        response.put("message", ex.getMessage());
        response.put("data", Map.of());
        
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }
    
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentialsException(BadCredentialsException ex) {
        log.error("Authentication failed: {}", ex.getMessage());
        
        Map<String, Object> response = new HashMap<>();
        response.put("code", 401);
        response.put("message", "사용자명 또는 비밀번호가 올바르지 않습니다.");
        response.put("data", Map.of());
        
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }
    
    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleUsernameNotFoundException(UsernameNotFoundException ex) {
        log.error("User not found: {}", ex.getMessage());
        
        Map<String, Object> response = new HashMap<>();
        response.put("code", 401);
        response.put("message", "사용자명 또는 비밀번호가 올바르지 않습니다.");
        response.put("data", Map.of());
        
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }
    
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFoundException(ResourceNotFoundException ex) {
        log.error("Resource not found: {}", ex.getMessage());
        
        Map<String, Object> response = new HashMap<>();
        response.put("code", 404);
        response.put("message", ex.getMessage());
        response.put("data", Map.of());
        
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }
    
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        log.error("Runtime error occurred: {}", ex.getMessage(), ex);
        
        Map<String, Object> response = new HashMap<>();
        response.put("code", 500);
        response.put("message", "데이터 처리 중 오류가 발생했습니다.");
        response.put("error", ex.getMessage());
        response.put("data", Map.of());
        
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        log.error("Unexpected error occurred: ", ex);
        
        Map<String, Object> response = new HashMap<>();
        response.put("code", 500);
        response.put("message", "서버 내부 오류가 발생했습니다.");
        response.put("error", ex.getMessage());
        response.put("data", Map.of());
        
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
} 