package com.fitnesslife.gym.model;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "chatbot_logs")
@CompoundIndex(name = "user_asked_idx", def = "{'userKey': 1, 'askedAt': -1}")
public class ChatLog {

    @Id
    private String id;

    private String userKey;

    private String question;
    private String answer;
    private Instant askedAt;
    private Instant answeredAt;

    public ChatLog(String userKey, String question, String answer, Instant askedAt, Instant answeredAt) {
        this.userKey = userKey;
        this.question = question;
        this.answer = answer;
        this.askedAt = askedAt;
        this.answeredAt = answeredAt;
    }

    public String getId() {
        return id;
    }

    public String getUserKey() {
        return userKey;
    }

    public String getQuestion() {
        return question;
    }

    public String getAnswer() {
        return answer;
    }

    public Instant getAskedAt() {
        return askedAt;
    }

    public Instant getAnsweredAt() {
        return answeredAt;
    }
}
