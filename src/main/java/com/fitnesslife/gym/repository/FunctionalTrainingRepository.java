package com.fitnesslife.gym.repository;

import com.fitnesslife.gym.model.FunctionalTraining;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;

@Repository
public interface FunctionalTrainingRepository extends MongoRepository<FunctionalTraining, String> {

    Optional<FunctionalTraining> findByIdFunctionalTraining(String id);

    Optional<FunctionalTraining> findByIdFunctionalTraining(int idFunctionalTraining);

    void deleteByIdFunctionalTraining(int idFunctionalTraining);

    boolean existsByIdFunctionalTraining(int idFunctionalTraining);

    List<FunctionalTraining> findByStatusOrderByDatetimeAsc(String status);

    @Query("{ 'datetime': { $gte: ?0, $lte: ?1 } }")
    List<FunctionalTraining> findByDatetimeBetween(LocalDateTime startDate, LocalDateTime endDate);

    @Query("{ 'datetime': { $gte: ?0, $lte: ?1 }, 'status': ?2 }")
    List<FunctionalTraining> findByDatetimeBetweenAndStatus(LocalDateTime startDate, LocalDateTime endDate,
            String status);

    @Query(value = "{ 'status': 'ACTIVE' }", sort = "{ 'userIds': -1 }")
    List<FunctionalTraining> findTopClasses(Pageable pageable);

    boolean existsByUserIdsContaining(Long identification);

    @Query(value = "{}", fields = "{ 'userIds': 1 }")
    List<FunctionalTraining> findAllUserIdProjections();
}
