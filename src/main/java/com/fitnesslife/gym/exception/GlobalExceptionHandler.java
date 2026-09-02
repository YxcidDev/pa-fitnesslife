package com.fitnesslife.gym.exception;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserNotFoundException.class)
    public String handleUserNotFoundException(UserNotFoundException ex, Model model) {
        log.error("Usuario no encontrado: {}", ex.getMessage());
        model.addAttribute("error", ex.getMessage());
        return "error/user-not-found";
    }

    @ExceptionHandler(Exception.class)
    public String handleGenericException(Exception ex, Model model) {
        log.error("Error inesperado: ", ex);
        model.addAttribute("error", "Ha ocurrido un error inesperado");
        return "error/generic-error";
    }
}
