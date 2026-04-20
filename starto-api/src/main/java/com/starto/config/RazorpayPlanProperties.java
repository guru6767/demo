package com.starto.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
@Data
@Component
@ConfigurationProperties(prefix = "razorpay")
public class RazorpayPlanProperties {

    private Key key;
    private Plans plans;

    @Data
    public static class Key {
        private String id;
        private String secret;
    }

    @Data
    public static class Plans {
        private String sprint;
        private String boost;
        private String pro;
        private String captain;
        private String captainPro;
        private String proPlus;
        private String growth;
        private String annual;
    }
    @PostConstruct
public void init() {
    System.out.println("SPRING CONFIG LOADED:");
    System.out.println("CAPTAIN PLAN = " + plans.getCaptain());
}
}