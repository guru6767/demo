package com.starto.config;

import com.starto.enums.Plan;
import com.starto.model.PlanEntity;
import com.starto.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class PlanDataLoader implements CommandLineRunner {

    private final PlanRepository planRepository;
    private final RazorpayPlanProperties razorpayPlanProperties;

    @Override
    public void run(String... args) {
        createIfNotExists(Plan.SPRINT, 5900, 7, "ONE_TIME");
        createIfNotExists(Plan.BOOST, 9900, 15, "ONE_TIME");
        createIfNotExists(Plan.PRO, 14900, 30, "RECURRING");
        createIfNotExists(Plan.PRO_PLUS, 34900, 90, "RECURRING");
        createIfNotExists(Plan.GROWTH, 57900, 180, "RECURRING");
        createIfNotExists(Plan.ANNUAL, 99900, 365, "RECURRING");
        createIfNotExists(Plan.CAPTAIN, 9900, 30, "RECURRING");
        createIfNotExists(Plan.CAPTAIN_PRO, 79900, 365, "RECURRING");
    }

    private void createIfNotExists(Plan code, int price, int days, String type) {
        planRepository.findByCode(code).orElseGet(() -> {
            PlanEntity p = new PlanEntity();
            p.setCode(code);
            p.setPricePaise(price);
            p.setDurationDays(days);
            p.setBillingType(Enum.valueOf(
                    com.starto.enums.BillingType.class, type));

            String razorpayPlanId = switch (code) {
                case SPRINT -> razorpayPlanProperties.getPlans().getSprint();
                case BOOST -> razorpayPlanProperties.getPlans().getBoost();
                case PRO -> razorpayPlanProperties.getPlans().getPro();
                case CAPTAIN -> razorpayPlanProperties.getPlans().getCaptain();
                case CAPTAIN_PRO -> razorpayPlanProperties.getPlans().getCaptainPro();
                case PRO_PLUS -> razorpayPlanProperties.getPlans().getProPlus();
                case GROWTH -> razorpayPlanProperties.getPlans().getGrowth();
                case ANNUAL -> razorpayPlanProperties.getPlans().getAnnual();
                default -> null;
            };

            p.setRazorpayPlanId(razorpayPlanId);

            return planRepository.save(p);
        });
    }
}