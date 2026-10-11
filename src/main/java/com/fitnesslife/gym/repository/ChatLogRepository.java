package com.fitnesslife.gym.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.fitnesslife.gym.model.ChatLog;

public interface ChatLogRepository extends MongoRepository<ChatLog, String> {

    List<ChatLog> findTop20ByUserKeyOrderByAskedAtDesc(String userKey);

    void deleteByUserKey(String userKey);
}
