package com.fitnesslife.gym.dto;

import java.time.Instant;

public record ChatMessageDto(String role, String content, Instant timestamp) {
}
