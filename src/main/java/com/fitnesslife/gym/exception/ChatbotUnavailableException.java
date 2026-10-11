package com.fitnesslife.gym.exception;

public class ChatbotUnavailableException extends RuntimeException {

    public ChatbotUnavailableException(Throwable cause) {
        super("El asistente no está disponible en este momento. Inténtalo de nuevo en unos minutos.", cause);
    }
}
