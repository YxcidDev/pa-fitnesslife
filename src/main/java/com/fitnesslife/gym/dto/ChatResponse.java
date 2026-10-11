package com.fitnesslife.gym.dto;

import java.time.Instant;

public record ChatResponse(String answer, Instant timestamp) {
}
