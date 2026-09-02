package com.fitnesslife.gym.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.fitnesslife.gym.model.Plan;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PlanRepository extends MongoRepository<Plan, String> {
    List<Plan> findAllByOrderByPriceAsc();

    Page<Plan> findAllByOrderByPriceAsc(Pageable pageable);
}
