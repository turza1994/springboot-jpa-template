package com.example.app.health;

import com.example.app.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class HealthController {

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> health(HttpServletRequest httpRequest) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("status", "UP");
        details.put("application", "backend-starter");

        return ResponseEntity.ok(ApiResponse.success(details, (String) httpRequest.getAttribute("requestId")));
    }

    @GetMapping("/ready")
    public ResponseEntity<ApiResponse<Map<String, Object>>> readiness(HttpServletRequest httpRequest) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("status", "UP");

        return ResponseEntity.ok(ApiResponse.success(details, (String) httpRequest.getAttribute("requestId")));
    }

    @GetMapping("/live")
    public ResponseEntity<ApiResponse<Map<String, Object>>> liveness(HttpServletRequest httpRequest) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("status", "UP");

        return ResponseEntity.ok(ApiResponse.success(details, (String) httpRequest.getAttribute("requestId")));
    }

}