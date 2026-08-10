package com.chuangyi.myofficedevice.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessException(BusinessException e) {
        log.warn("Business exception: {}", e.getMessage());
        return ResponseEntity.status(e.getCode())
                .body(Map.of("code", e.getCode(), "message", e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(Exception e) {
        log.error("Unexpected exception", e);
        return ResponseEntity.internalServerError()
                .body(Map.of("code", 500, "message", "Internal Server Error"));
    }
}
