package com.fitnesslife.gym.service;

import com.fitnesslife.gym.dto.ChurnResultDTO;
import com.fitnesslife.gym.enums.Role;
import com.fitnesslife.gym.model.*;
import com.fitnesslife.gym.repository.*;
import com.fitnesslife.gym.service.ChurnAggregationService.AttendanceFeatures;
import com.fitnesslife.gym.service.ChurnAggregationService.PaymentFeatures;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import weka.classifiers.Classifier;
import weka.core.*;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChurnPredictionService {

    private final UserRepository userRepository;
    private final FunctionalTrainingRepository functionalTrainingRepository;
    private final ChurnPredictionRepository churnPredictionRepository;
    private final ChurnAggregationService aggregationService;

    private Classifier classifier;
    private Instances datasetStructure;

    private static final int HIGH_RISK_DAYS_WITHOUT_ATT = 30;
    private static final int MEDIUM_RISK_DAYS_WITHOUT_ATT = 15;
    private static final int LOW_ATT_COUNT = 3;
    private static final double LOW_SESSION_MINUTES = 30.0;
    private static final int PROFILE_HIGH_ATT = 16;
    private static final double PROFILE_HIGH_MIN = 45.0;
    private static final int PROFILE_LOW_ATT = 8;
    private static final double PROFILE_LOW_MIN = 25.0;

    @PostConstruct
    public void loadModel() {
        try {
            InputStream modelStream = getClass().getResourceAsStream("/model/churn_model.model");
            if (modelStream == null)
                throw new RuntimeException("Modelo no encontrado en /model/churn_model.model");
            classifier = (Classifier) SerializationHelper.read(modelStream);
            datasetStructure = buildDatasetStructure();
            log.info("[ChurnService] Modelo J48 cargado. Atributos: {}",
                    datasetStructure.numAttributes());
        } catch (Exception e) {
            throw new RuntimeException("Error al cargar modelo churn: " + e.getMessage(), e);
        }
    }

    public List<ChurnPrediction> getCachedPredictions() {
        return churnPredictionRepository.findAll();
    }

    public List<ChurnResultDTO> recalculateAndPersist() {
        long start = System.currentTimeMillis();
        log.info("[ChurnService] Iniciando recálculo...");

        List<User> activeUsers = userRepository.findByRoleAndIsActive(Role.USER, true);
        if (activeUsers.isEmpty()) {
            log.warn("[ChurnService] Sin usuarios activos.");
            return Collections.emptyList();
        }

        List<String> userIds = activeUsers.stream()
                .map(User::getId).collect(Collectors.toList());
        log.info("[ChurnService] Usuarios activos: {}", activeUsers.size());

        Map<String, AttendanceFeatures> attFeatures = aggregationService.getAttendanceFeatures(userIds);
        Map<String, PaymentFeatures> payFeatures = aggregationService.getPaymentFeatures(userIds);
        Set<Long> usersInClasses = loadUsersInClasses();

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextRun = now.plusHours(6);

        List<ChurnResultDTO> results = new ArrayList<>(activeUsers.size());
        List<ChurnPrediction> toSave = new ArrayList<>(activeUsers.size());

        for (User user : activeUsers) {
            try {
                ChurnResultDTO dto = buildChurnResult(user, attFeatures, payFeatures, usersInClasses, now);
                results.add(dto);
                toSave.add(toEntity(dto, now, nextRun));
            } catch (Exception ex) {
                log.error("[ChurnService] Error procesando usuario {}: {}",
                        user.getEmail(), ex.getMessage(), ex);
            }
        }

        results.sort(Comparator.comparingInt(r -> riskOrder(r.getRiskLevel())));
        persistPredictions(toSave);

        long elapsed = System.currentTimeMillis() - start;
        log.info("[ChurnService] Recálculo completado: {} usuarios en {} ms", results.size(), elapsed);
        return results;
    }

    private ChurnResultDTO buildChurnResult(
            User user,
            Map<String, AttendanceFeatures> attMap,
            Map<String, PaymentFeatures> payMap,
            Set<Long> usersInClasses,
            LocalDateTime now) {

        ChurnResultDTO result = new ChurnResultDTO();
        result.setUserId(user.getId());
        result.setName(user.getName() + " " + user.getLastname());
        result.setEmail(user.getEmail());
        result.setPhone(user.getPhone());

        String planName = normalizePlan(user.getPlan());
        int planLevel = resolvePlanLevel(planName);
        result.setPlan(planName);
        result.setPlanLevel(planLevel);

        AttendanceFeatures att = attMap.getOrDefault(user.getId(), emptyAttFeatures(user.getId()));
        int nAttendances = att.getNAttendances();
        double avgSessionMinutes = Math.max(0.0, att.getAvgSessionMinutes());
        long daysSinceLastAtt = att.getLastCheckIn() != null
                ? Math.max(0L, ChronoUnit.DAYS.between(att.getLastCheckIn(), now))
                : 90L;
        result.setNAttendances(nAttendances);
        result.setAvgSessionMinutes(avgSessionMinutes);
        result.setDaysSinceLastAtt(daysSinceLastAtt);

        PaymentFeatures pay = payMap.getOrDefault(user.getId(), emptyPayFeatures(user.getId()));
        int nPayments = pay.getNPayments();
        long daysSinceLastPayment = pay.getLastValidUntil() != null
                ? Math.max(0L, ChronoUnit.DAYS.between(pay.getLastValidUntil(), now))
                : 90L;
        result.setNPayments(nPayments);
        result.setDaysSinceLastPayment(daysSinceLastPayment);

        boolean participates = usersInClasses.contains(user.getIdentification());
        result.setParticipatesInClasses(participates);

        String profile = inferProfile(nAttendances, avgSessionMinutes);
        result.setProfile(profile);

        PredictionResult wekaResult = predictWithConfidence(
                planName, planLevel, profile,
                nPayments, daysSinceLastPayment,
                nAttendances, avgSessionMinutes,
                daysSinceLastAtt, participates);

        result.setPrediction(wekaResult.prediction);
        result.setPredictionConfidence(wekaResult.confidence);

        log.debug("[ChurnService] Usuario={} pred={} conf={}%",
                user.getEmail(), wekaResult.prediction,
                String.format("%.1f", wekaResult.confidence));

        String riskLevel = classifyRisk(wekaResult.prediction, daysSinceLastAtt, nAttendances);
        result.setRiskLevel(riskLevel);
        result.setReasons(buildReasons(planLevel, nAttendances, daysSinceLastAtt,
                participates, avgSessionMinutes, daysSinceLastPayment));
        result.setRecommendations(buildRecommendations(riskLevel));

        return result;
    }

    private static class PredictionResult {
        final String prediction;
        final double confidence;

        PredictionResult(String prediction, double confidence) {
            this.prediction = prediction;
            this.confidence = confidence;
        }
    }

    private PredictionResult predictWithConfidence(
            String plan, int planLevel, String profile,
            int nPayments, long daysSinceLastPayment,
            int nAttendances, double avgSessionMinutes,
            long daysSinceLastAtt, boolean participatesInClasses) {

        try {
            Instance instance = buildInstance(
                    plan, planLevel, profile,
                    nPayments, daysSinceLastPayment,
                    nAttendances, avgSessionMinutes,
                    daysSinceLastAtt, participatesInClasses);

            int classIndex = (int) classifier.classifyInstance(instance);
            String prediction = datasetStructure.classAttribute().value(classIndex);

            double[] distribution = classifier.distributionForInstance(instance);
            double confidence = (classIndex < distribution.length)
                    ? Math.round(distribution[classIndex] * 1000.0) / 10.0
                    : 50.0;

            return new PredictionResult(prediction, confidence);

        } catch (Exception e) {
            log.error("[ChurnService] Error en clasificación Weka: {}", e.getMessage(), e);
            return new PredictionResult("Yes", 50.0);
        }
    }

    private Instance buildInstance(
            String plan, int planLevel, String profile,
            int nPayments, long daysSinceLastPayment,
            int nAttendances, double avgSessionMinutes,
            long daysSinceLastAtt, boolean participatesInClasses) {

        Instance instance = new DenseInstance(datasetStructure.numAttributes());
        instance.setDataset(datasetStructure);

        Attribute planAttr = datasetStructure.attribute(0);
        if (planAttr.indexOfValue(plan) < 0)
            plan = "Básico";
        instance.setValue(planAttr, plan);

        instance.setValue(1, planLevel);

        Attribute profileAttr = datasetStructure.attribute(2);
        instance.setValue(profileAttr, profile);

        instance.setValue(3, nPayments);
        instance.setValue(4, daysSinceLastPayment);
        instance.setValue(5, nAttendances);
        instance.setValue(6, avgSessionMinutes);
        instance.setValue(7, daysSinceLastAtt);
        instance.setValue(8, participatesInClasses ? 1.0 : 0.0);
        instance.setClassMissing();

        return instance;
    }

    private void persistPredictions(List<ChurnPrediction> predictions) {
        for (ChurnPrediction p : predictions) {
            try {
                churnPredictionRepository.findByUserId(p.getUserId())
                        .ifPresentOrElse(
                                existing -> {
                                    p.setId(existing.getId());
                                    churnPredictionRepository.save(p);
                                },
                                () -> churnPredictionRepository.save(p));
            } catch (Exception ex) {
                log.error("[ChurnService] Error persistiendo userId={}: {}", p.getUserId(), ex.getMessage());
            }
        }
        log.info("[ChurnService] {} predicciones guardadas en churnPredictions", predictions.size());
    }

    private ChurnPrediction toEntity(ChurnResultDTO dto, LocalDateTime now, LocalDateTime nextRun) {
        return ChurnPrediction.builder()
                .userId(dto.getUserId())
                .name(dto.getName())
                .email(dto.getEmail())
                .phone(dto.getPhone())
                .plan(dto.getPlan())
                .planLevel(dto.getPlanLevel())
                .profile(dto.getProfile())
                .nPayments(dto.getNPayments())
                .daysSinceLastPayment(dto.getDaysSinceLastPayment())
                .nAttendances(dto.getNAttendances())
                .avgSessionMinutes(dto.getAvgSessionMinutes())
                .daysSinceLastAtt(dto.getDaysSinceLastAtt())
                .participatesInClasses(dto.isParticipatesInClasses())
                .prediction(dto.getPrediction())
                .predictionConfidence(dto.getPredictionConfidence())
                .riskLevel(dto.getRiskLevel())
                .riskOrder(riskOrder(dto.getRiskLevel()))
                .reasons(dto.getReasons())
                .recommendations(dto.getRecommendations())
                .calculatedAt(now)
                .nextUpdateAt(nextRun)
                .build();
    }

    private Instances buildDatasetStructure() {
        ArrayList<Attribute> attributes = new ArrayList<>();

        ArrayList<String> planValues = new ArrayList<>();
        planValues.add("Premium");
        planValues.add("Básico");
        planValues.add("Elite");
        attributes.add(new Attribute("plan", planValues));

        attributes.add(new Attribute("plan_level"));

        ArrayList<String> profileValues = new ArrayList<>();
        profileValues.add("comprometido");
        profileValues.add("desmotivado");
        profileValues.add("irregular");
        attributes.add(new Attribute("profile", profileValues));

        attributes.add(new Attribute("n_payments"));
        attributes.add(new Attribute("days_since_last_payment"));
        attributes.add(new Attribute("n_attendances"));
        attributes.add(new Attribute("avg_session_minutes"));
        attributes.add(new Attribute("days_since_last_att"));
        attributes.add(new Attribute("participates_in_classes"));

        ArrayList<String> churnValues = new ArrayList<>();
        churnValues.add("No");
        churnValues.add("Yes");
        attributes.add(new Attribute("churn", churnValues));

        Instances structure = new Instances("ChurnDataset", attributes, 0);
        structure.setClassIndex(structure.numAttributes() - 1);
        return structure;
    }

    private Set<Long> loadUsersInClasses() {
        return functionalTrainingRepository.findAllUserIdProjections().stream()
                .flatMap(ft -> ft.getUserIds().stream())
                .collect(Collectors.toSet());
    }

    private String classifyRisk(String prediction, long daysSinceLastAtt, int nAttendances) {
        boolean churnPredicted = "Yes".equals(prediction);
        boolean lowActivity = daysSinceLastAtt > HIGH_RISK_DAYS_WITHOUT_ATT || nAttendances <= LOW_ATT_COUNT;
        boolean borderlineActivity = daysSinceLastAtt > MEDIUM_RISK_DAYS_WITHOUT_ATT;
        if (churnPredicted && lowActivity)
            return "HIGH";
        if (churnPredicted || borderlineActivity)
            return "MEDIUM";
        return "LOW";
    }

    private List<String> buildReasons(int planLevel, int nAttendances, long daysSinceLastAtt,
            boolean participatesInClasses, double avgSessionMinutes, long daysSinceLastPayment) {
        List<String> reasons = new ArrayList<>();
        if (daysSinceLastAtt > HIGH_RISK_DAYS_WITHOUT_ATT)
            reasons.add("Más de " + HIGH_RISK_DAYS_WITHOUT_ATT + " días sin asistir al gimnasio");
        else if (daysSinceLastAtt > MEDIUM_RISK_DAYS_WITHOUT_ATT)
            reasons.add("Más de " + MEDIUM_RISK_DAYS_WITHOUT_ATT + " días sin asistir");
        if (nAttendances <= LOW_ATT_COUNT)
            reasons.add("Frecuencia de asistencia muy baja (" + nAttendances + " visitas en total)");
        if (!participatesInClasses)
            reasons.add("No participa en clases funcionales grupales");
        if (planLevel == 1)
            reasons.add("Plan Básico — menor nivel de compromiso con el gimnasio");
        if (avgSessionMinutes < LOW_SESSION_MINUTES && avgSessionMinutes > 0)
            reasons.add("Sesiones de corta duración (promedio " + String.format("%.0f", avgSessionMinutes) + " min)");
        if (daysSinceLastPayment > 25)
            reasons.add("La membresía venció hace " + daysSinceLastPayment + " días");
        if (reasons.isEmpty())
            reasons.add("Perfil de comportamiento analizado por el modelo predictivo");
        return reasons;
    }

    private List<String> buildRecommendations(String riskLevel) {
        List<String> recs = new ArrayList<>();
        switch (riskLevel) {
            case "HIGH" -> {
                recs.add("Contacto directo urgente: llamar o escribir al usuario");
                recs.add("Ofrecer descuento personalizado en la próxima renovación");
                recs.add("Agendar sesión de evaluación o consulta gratuita");
                recs.add("Asignar seguimiento a un entrenador de forma prioritaria");
            }
            case "MEDIUM" -> {
                recs.add("Enviar recordatorio de asistencia por correo o mensaje");
                recs.add("Ofrecer promoción ligera en renovación del plan");
                recs.add("Invitar a participar en una clase funcional grupal");
            }
            default -> {
                recs.add("Incluir en programa de fidelización");
                recs.add("Enviar encuesta de satisfacción para mantener el vínculo");
                recs.add("Reconocimiento de logros o metas de asistencia");
            }
        }
        return recs;
    }

    private String inferProfile(int nAttendances, double avgSessionMinutes) {
        if (nAttendances == 0)
            return "desmotivado";
        boolean highAtt = nAttendances >= PROFILE_HIGH_ATT;
        boolean highMin = avgSessionMinutes >= PROFILE_HIGH_MIN;
        boolean lowAtt = nAttendances < PROFILE_LOW_ATT;
        boolean lowMin = avgSessionMinutes > 0 && avgSessionMinutes < PROFILE_LOW_MIN;
        if (highAtt && highMin)
            return "comprometido";
        if (lowAtt || lowMin)
            return "desmotivado";
        return "irregular";
    }

    private String normalizePlan(String plan) {
        if (plan == null)
            return "Básico";
        String p = plan.trim();
        if (p.equalsIgnoreCase("Elite"))
            return "Elite";
        if (p.equalsIgnoreCase("Premium"))
            return "Premium";
        return "Básico";
    }

    private int resolvePlanLevel(String plan) {
        if ("Elite".equals(plan))
            return 3;
        if ("Premium".equals(plan))
            return 2;
        return 1;
    }

    private int riskOrder(String riskLevel) {
        return switch (riskLevel) {
            case "HIGH" -> 0;
            case "MEDIUM" -> 1;
            default -> 2;
        };
    }

    private AttendanceFeatures emptyAttFeatures(String userId) {
        AttendanceFeatures f = new AttendanceFeatures();
        f.setUserId(userId);
        f.setNAttendances(0);
        f.setLastCheckIn(null);
        f.setAvgSessionMinutes(0.0);
        return f;
    }

    private PaymentFeatures emptyPayFeatures(String userId) {
        PaymentFeatures f = new PaymentFeatures();
        f.setUserId(userId);
        f.setNPayments(0);
        f.setLastValidUntil(null);
        return f;
    }
}