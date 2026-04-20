package com.starto;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.starto.config.RazorpayPlanProperties;

import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(RazorpayPlanProperties.class)
public class StartoApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(StartoApiApplication.class, args);
    }

}
