package com.fitnesslife.gym.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChurnAggregationService {

    private final MongoTemplate mongoTemplate;

    public Map<String, AttendanceFeatures> getAttendanceFeatures(List<String> userIds) {
        log.debug("[ChurnAgg] Calculando features de asistencia para {} usuarios", userIds.size());

        MatchOperation match = Aggregation.match(
                Criteria.where("userId").in(userIds).and("result").is("ALLOWED"));

        GroupOperation group = Aggregation.group("userId")
                .count().as("nAttendances")
                .max("checkIn").as("lastCheckIn")
                .avg(
                        ConditionalOperators.when(
                                Criteria.where("checkOut").ne(null)).thenValueOf(
                                        ArithmeticOperators.Subtract.valueOf("checkOut").subtract("checkIn"))
                                .otherwise(0))
                .as("avgDurationMs");

        ProjectionOperation project = Aggregation.project()
                .and("_id").as("userId")
                .and("nAttendances").as("nAttendances")
                .and("lastCheckIn").as("lastCheckIn")
                .and(
                        ArithmeticOperators.Divide
                                .valueOf("avgDurationMs")
                                .divideBy(60000))
                .as("avgSessionMinutes");

        Aggregation pipeline = Aggregation.newAggregation(match, group, project);

        List<Document> results = mongoTemplate
                .aggregate(pipeline, "attendances", Document.class)
                .getMappedResults();

        Map<String, AttendanceFeatures> featuresMap = new HashMap<>(results.size() * 2);
        for (Document doc : results) {
            String uid = doc.getString("userId");
            if (uid == null)
                continue;

            AttendanceFeatures f = new AttendanceFeatures();
            f.setUserId(uid);
            f.setNAttendances(doc.getInteger("nAttendances", 0));
            f.setLastCheckIn(doc.getDate("lastCheckIn") != null
                    ? doc.getDate("lastCheckIn").toInstant()
                            .atZone(java.time.ZoneId.systemDefault()).toLocalDateTime()
                    : null);
            Object avgObj = doc.get("avgSessionMinutes");
            f.setAvgSessionMinutes(avgObj instanceof Number ? ((Number) avgObj).doubleValue() : 0.0);

            featuresMap.put(uid, f);
        }

        log.debug("[ChurnAgg] Asistencias agregadas: {} usuarios con datos", featuresMap.size());
        return featuresMap;
    }

    public Map<String, PaymentFeatures> getPaymentFeatures(List<String> userIds) {
        log.debug("[ChurnAgg] Calculando features de pagos para {} usuarios", userIds.size());

        MatchOperation match = Aggregation.match(
                Criteria.where("userId").in(userIds).and("status").is("ACCEPTED"));

        GroupOperation group = Aggregation.group("userId")
                .count().as("nPayments")
                .max("validUntil").as("lastValidUntil");

        ProjectionOperation project = Aggregation.project()
                .and("_id").as("userId")
                .and("nPayments").as("nPayments")
                .and("lastValidUntil").as("lastValidUntil");

        Aggregation pipeline = Aggregation.newAggregation(match, group, project);

        List<Document> results = mongoTemplate
                .aggregate(pipeline, "payments", Document.class)
                .getMappedResults();

        Map<String, PaymentFeatures> featuresMap = new HashMap<>(results.size() * 2);
        for (Document doc : results) {
            String uid = doc.getString("userId");
            if (uid == null)
                continue;

            PaymentFeatures f = new PaymentFeatures();
            f.setUserId(uid);
            f.setNPayments(doc.getInteger("nPayments", 0));
            f.setLastValidUntil(doc.getDate("lastValidUntil") != null
                    ? doc.getDate("lastValidUntil").toInstant()
                            .atZone(java.time.ZoneId.systemDefault()).toLocalDateTime()
                    : null);

            featuresMap.put(uid, f);
        }

        log.debug("[ChurnAgg] Pagos agregados: {} usuarios con datos", featuresMap.size());
        return featuresMap;
    }

    @lombok.Data
    public static class AttendanceFeatures {
        private String userId;
        private int nAttendances;
        private java.time.LocalDateTime lastCheckIn;
        private double avgSessionMinutes;
    }

    @lombok.Data
    public static class PaymentFeatures {
        private String userId;
        private int nPayments;
        private java.time.LocalDateTime lastValidUntil;
    }
}