package com.fitnesslife.gym.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatRequest(
        @NotBlank(message = "Escribe un mensaje.")
        @Size(max = 500, message = "El mensaje no puede superar los 500 caracteres.")
        String message) {
}
