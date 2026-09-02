package com.fitnesslife.gym.service;

import com.fitnesslife.gym.enums.Sex;
import com.fitnesslife.gym.model.Attendance;
import com.fitnesslife.gym.model.FunctionalTraining;
import com.fitnesslife.gym.model.Payment;
import com.fitnesslife.gym.repository.AttendanceRepository;
import com.fitnesslife.gym.repository.FunctionalTrainingRepository;
import com.fitnesslife.gym.repository.PaymentRepository;
import com.fitnesslife.gym.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;
    private final AttendanceRepository attendanceRepository;
    private final FunctionalTrainingRepository functionalTrainingRepository;

    public Map<String, Object> getMainMetrics() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfToday = now.toLocalDate().atStartOfDay();
        LocalDateTime endOfToday = startOfToday.plusDays(1);
        LocalDateTime startOfMonth = now.with(TemporalAdjusters.firstDayOfMonth()).toLocalDate().atStartOfDay();
        LocalDateTime startOfLastMonth = startOfMonth.minusMonths(1);
        LocalDateTime endOfLastMonth = startOfMonth;

        Map<String, Object> metrics = new HashMap<>();

        long activeUsers = userRepository.countActiveUsers();
        long activeUsersLastMonth = userRepository.countActiveUsersBetween(startOfLastMonth, endOfLastMonth);
        double userGrowth = calculateGrowthPercentage(activeUsers, activeUsersLastMonth);

        long accessesToday = attendanceRepository.countAllowedAccessesBetween(startOfToday, endOfToday);
        long accessesYesterday = attendanceRepository.countAllowedAccessesBetween(startOfToday.minusDays(1),
                startOfToday);
        double accessGrowth = calculateGrowthPercentage(accessesToday, accessesYesterday);

        List<Payment> paymentsThisMonth = paymentRepository.findAcceptedPaymentsBetween(startOfMonth, now);
        double revenueThisMonth = paymentsThisMonth.stream().mapToDouble(Payment::getAmount).sum();
        List<Payment> paymentsLastMonth = paymentRepository.findAcceptedPaymentsBetween(startOfLastMonth,
                endOfLastMonth);
        double revenueLastMonth = paymentsLastMonth.stream().mapToDouble(Payment::getAmount).sum();
        double revenueGrowth = calculateGrowthPercentage(revenueThisMonth, revenueLastMonth);

        long activePlans = paymentRepository.countActivePlans(now);
        long activePlansLastMonth = paymentRepository.countActivePlans(startOfMonth);
        double plansGrowth = calculateGrowthPercentage(activePlans, activePlansLastMonth);

        metrics.put("activeUsers", Map.of("value", activeUsers, "growth", userGrowth));
        metrics.put("accessesToday", Map.of("value", accessesToday, "growth", accessGrowth));
        metrics.put("revenueThisMonth", Map.of("value", revenueThisMonth, "growth", revenueGrowth));
        metrics.put("activePlans", Map.of("value", activePlans, "growth", plansGrowth));

        return metrics;
    }

    public List<Map<String, Object>> getMonthlyRevenue() {
        LocalDateTime now = LocalDateTime.now();
        List<Map<String, Object>> monthlyData = new ArrayList<>();

        for (int i = 5; i >= 0; i--) {
            LocalDateTime monthStart = now.minusMonths(i)
                    .with(TemporalAdjusters.firstDayOfMonth()).toLocalDate().atStartOfDay();
            LocalDateTime monthEnd = monthStart.plusMonths(1);

            List<Payment> payments = paymentRepository.findAcceptedPaymentsBetween(monthStart, monthEnd);
            double revenue = payments.stream().mapToDouble(Payment::getAmount).sum();

            Map<String, Object> data = new HashMap<>();
            data.put("month", monthStart.getMonth().getDisplayName(TextStyle.SHORT, new Locale("es", "ES")));
            data.put("revenue", revenue);
            monthlyData.add(data);
        }

        return monthlyData;
    }

    public List<Map<String, Object>> getRecentTransactions() {
        List<Payment> recentPayments = paymentRepository.findRecentPayments(PageRequest.of(0, 5));

        return recentPayments.stream()
                .map(payment -> {
                    Map<String, Object> t = new HashMap<>();
                    t.put("userName", payment.getUserName());
                    t.put("planName", payment.getPlanName());
                    t.put("amount", payment.getAmount());
                    t.put("date", payment.getTransactionDate());
                    t.put("status", payment.getStatus());
                    return t;
                })
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> getPlanSalesDistribution() {
        LocalDateTime threeMonthsAgo = LocalDateTime.now().minusMonths(3);
        List<Payment> payments = paymentRepository.findAcceptedPaymentsForStats(threeMonthsAgo, LocalDateTime.now());

        Map<String, Long> planCounts = payments.stream()
                .filter(p -> p.getPlanName() != null)
                .collect(Collectors.groupingBy(Payment::getPlanName, Collectors.counting()));

        long total = planCounts.values().stream().mapToLong(Long::longValue).sum();

        return planCounts.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("planName", entry.getKey());
                    data.put("count", entry.getValue());
                    data.put("percentage", total > 0 ? (entry.getValue() * 100.0 / total) : 0);
                    return data;
                })
                .sorted((a, b) -> Long.compare((Long) b.get("count"), (Long) a.get("count")))
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> getAccessByDayOfWeek() {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        List<Attendance> attendances = attendanceRepository
                .findAllowedAccessesBetween(thirtyDaysAgo, LocalDateTime.now());

        Map<DayOfWeek, Long> dayCount = attendances.stream()
                .filter(a -> a.getCheckIn() != null)
                .collect(Collectors.groupingBy(
                        a -> a.getCheckIn().getDayOfWeek(),
                        Collectors.counting()));

        List<Map<String, Object>> result = new ArrayList<>();
        for (DayOfWeek day : DayOfWeek.values()) {
            Map<String, Object> data = new HashMap<>();
            data.put("day", day.getDisplayName(TextStyle.SHORT, new Locale("es", "ES")));
            data.put("count", dayCount.getOrDefault(day, 0L));
            result.add(data);
        }
        return result;
    }

    public List<Map<String, Object>> getGenderDistribution() {
        List<Map<String, Object>> distribution = new ArrayList<>();
        for (Sex sex : Sex.values()) {
            long count = userRepository.countBySexAndIsActive(sex.name());
            Map<String, Object> data = new HashMap<>();
            data.put("gender", sex.name());
            data.put("count", count);
            distribution.add(data);
        }
        return distribution;
    }

    public List<Map<String, Object>> getPopularClasses() {
        List<FunctionalTraining> topClasses = functionalTrainingRepository.findTopClasses(PageRequest.of(0, 5));
        return topClasses.stream()
                .map(training -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("name", training.getNameTraining());
                    data.put("instructor", training.getInstructor());
                    data.put("enrolled", training.getUserIds() != null ? training.getUserIds().size() : 0);
                    data.put("capacity", training.getMaximumCapacity());
                    return data;
                })
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> getExpiringPlans() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime sevenDaysLater = now.plusDays(7);
        List<Payment> expiringPayments = paymentRepository.findExpiringPlans(now, sevenDaysLater);

        return expiringPayments.stream()
                .map(payment -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("userName", payment.getUserName());
                    data.put("planName", payment.getPlanName());
                    data.put("expirationDate", payment.getValidUntil());
                    data.put("daysRemaining",
                            java.time.temporal.ChronoUnit.DAYS.between(now, payment.getValidUntil()));
                    return data;
                })
                .sorted((a, b) -> Long.compare((Long) a.get("daysRemaining"), (Long) b.get("daysRemaining")))
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> getPeakHoursHeatmap() {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        List<Attendance> attendances = attendanceRepository
                .findAllowedAccessesForPeakHours(thirtyDaysAgo, LocalDateTime.now());

        String[] daysOrder = { "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom" };
        Map<String, Map<String, Long>> heatmapData = new HashMap<>();

        for (String day : daysOrder) {
            Map<String, Long> hourMap = new HashMap<>();
            for (int hour = 6; hour <= 22; hour++) {
                hourMap.put(String.format("%02d:00", hour), 0L);
            }
            heatmapData.put(day, hourMap);
        }

        for (Attendance attendance : attendances) {
            if (attendance.getCheckIn() == null)
                continue;

            String dayName = attendance.getCheckIn().getDayOfWeek()
                    .getDisplayName(TextStyle.SHORT, new Locale("es", "ES"));
            dayName = dayName.substring(0, 1).toUpperCase() + dayName.substring(1, 3);

            int hour = attendance.getCheckIn().getHour();
            String hourStr = String.format("%02d:00", hour);

            if (hour >= 6 && hour <= 22 && heatmapData.containsKey(dayName)) {
                heatmapData.get(dayName).merge(hourStr, 1L, Long::sum);
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (String day : daysOrder) {
            Map<String, Long> hourMap = heatmapData.get(day);
            for (int hour = 6; hour <= 22; hour++) {
                String hourStr = String.format("%02d:00", hour);
                Map<String, Object> dataPoint = new HashMap<>();
                dataPoint.put("x", day);
                dataPoint.put("y", hourStr);
                dataPoint.put("v", hourMap.get(hourStr));
                result.add(dataPoint);
            }
        }
        return result;
    }

    private double calculateGrowthPercentage(double current, double previous) {
        if (previous == 0)
            return current > 0 ? 100.0 : 0.0;
        return ((current - previous) / previous) * 100.0;
    }

    private double calculateGrowthPercentage(long current, long previous) {
        return calculateGrowthPercentage((double) current, (double) previous);
    }
}