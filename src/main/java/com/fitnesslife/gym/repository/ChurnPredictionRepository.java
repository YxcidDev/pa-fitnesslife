package com.fitnesslife.gym.repository;

import com.fitnesslife.gym.model.ChurnPrediction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChurnPredictionRepository extends MongoRepository<ChurnPrediction, String> {

    Optional<ChurnPrediction> findByUserId(String userId);

    Page<ChurnPrediction> findAll(Pageable pageable);

    Page<ChurnPrediction> findByRiskLevel(String riskLevel, Pageable pageable);

    Page<ChurnPrediction> findByProfile(String profile, Pageable pageable);

    Page<ChurnPrediction> findByRiskLevelAndProfile(String riskLevel, String profile, Pageable pageable);

    @Query("{ 'name': { $regex: ?0, $options: 'i' } }")
    Page<ChurnPrediction> searchByName(String name, Pageable pageable);

    @Query("{ 'riskLevel': ?0, 'name': { $regex: ?1, $options: 'i' } }")
    Page<ChurnPrediction> searchByNameAndRisk(String riskLevel, String name, Pageable pageable);

    @Query("{ 'profile': ?0, 'name': { $regex: ?1, $options: 'i' } }")
    Page<ChurnPrediction> searchByNameAndProfile(String profile, String name, Pageable pageable);

    @Query("{ 'riskLevel': ?0, 'profile': ?1, 'name': { $regex: ?2, $options: 'i' } }")
    Page<ChurnPrediction> searchByNameAndRiskAndProfile(String riskLevel, String profile, String name,
            Pageable pageable);

    List<ChurnPrediction> findByRiskLevel(String riskLevel);

    long countByRiskLevel(String riskLevel);

    long countByPrediction(String prediction);
}