package com.fitnesslife.gym.exception;

public class RateLimitExceededException extends RuntimeException {

    public RateLimitExceededException() {
        super("Estás enviando mensajes muy rápido. Espera un momento e inténtalo de nuevo.");
    }
}
