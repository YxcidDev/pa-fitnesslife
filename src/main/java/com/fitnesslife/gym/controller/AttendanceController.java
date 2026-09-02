package com.fitnesslife.gym.controller;

import com.fitnesslife.gym.service.AttendanceService;
import com.fitnesslife.gym.service.AttendanceService.AttendanceResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @GetMapping("/attendance/kiosk")
    public String kioskView() {
        return "attendance/kiosk";
    }

    @PostMapping("/api/attendance/verify")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> verifyAttendance(
            @RequestBody Map<String, String> body) {

        String qrCode = body.get("qrCode");
        log.info("Received attendance verification request for QR: {}", qrCode);

        if (qrCode == null || qrCode.isBlank()) {
            log.warn("Empty or null QR code received");
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "status", "ERROR",
                            "message", "Código QR inválido"));
        }

        try {
            AttendanceResult result = attendanceService.processQrScan(qrCode.trim());

            Map<String, Object> response = new HashMap<>();
            response.put("status", result.status().name());
            response.put("message", result.message());
            response.put("userName", result.userName());
            response.put("userPhoto", result.userPhoto());
            response.put("userPlan", result.userPlan());
            response.put("userId", result.userId());

            log.info("Attendance result: {} for QR: {}", result.status(), qrCode);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error processing attendance for QR: {}", qrCode, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "status", "ERROR",
                            "message", "Error interno al procesar el acceso"));
        }
    }

    @GetMapping("/api/attendance/active-count")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getActiveCount() {
        int count = attendanceService.getActiveGymCount();
        return ResponseEntity.ok(Map.of("count", count));
    }
}