package com.fitnesslife.gym.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "churnPredictions")
@CompoundIndexes({
        @CompoundIndex(name = "riskLevel_1_calculatedAt_-1", def = "{'riskLevel': 1, 'calculatedAt': -1}")
})
public class ChurnPrediction {

    @Id
    private String id;

    @Indexed(unique = true)
    private String userId;

    private String name;
    private String email;
    private String phone;
    private String plan;
    private int planLevel;
    private String profile;
    private int nPayments;
    private long daysSinceLastPayment;
    private int nAttendances;
    private double avgSessionMinutes;
    private long daysSinceLastAtt;
    private boolean participatesInClasses;
    private String prediction;
    private double predictionConfidence;
    private String riskLevel;
    private int riskOrder;
    private List<String> reasons;
    private List<String> recommendations;
    private LocalDateTime calculatedAt;
    private LocalDateTime nextUpdateAt;
}