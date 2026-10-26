package edu.campus.auction.web;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {
    @GetMapping("/api/health")
    public Map<String, String> health() {
        return Map.of("status", "UP", "stage", "MVP",
                "message", "校园二手拍卖后端已启动；付款功能仅作模拟");
    }
}
