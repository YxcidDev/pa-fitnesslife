package com.fitnesslife.gym.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.fitnesslife.gym.dto.ApiError;
import com.fitnesslife.gym.exception.ChatbotUnavailableException;
import com.fitnesslife.gym.exception.RateLimitExceededException;

@RestControllerAdvice(assignableTypes = ChatbotController.class)
public class ChatbotExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> invalid(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("Mensaje inválido.");
        return ResponseEntity.badRequest().body(new ApiError(message));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> unreadable(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest().body(new ApiError("Solicitud inválida."));
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ApiError> tooManyRequests(RateLimitExceededException e) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(new ApiError(e.getMessage()));
    }

    @ExceptionHandler(ChatbotUnavailableException.class)
    public ResponseEntity<ApiError> unavailable(ChatbotUnavailableException e) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new ApiError(e.getMessage()));
    }
}
