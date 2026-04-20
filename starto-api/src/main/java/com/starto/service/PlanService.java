package com.starto.service;

import com.starto.config.PlanConfig;
import com.starto.enums.Plan;
import org.springframework.stereotype.Service;

@Service
public class PlanService {

    private static final int UNLIMITED = -1;

    // 🔹 Generic limit checker (core logic)
   private boolean isAllowed(int limit, int used) {
    return limit == Integer.MAX_VALUE || used < limit;
}

    //  SIGNALS
    public boolean canPostSignal(Plan plan, int activeSignals) {
        int limit = PlanConfig.MAX_SIGNALS.getOrDefault(plan, 0);
        return isAllowed(limit, activeSignals);
    }

    public int getSignalLimit(Plan plan) {
        return PlanConfig.MAX_SIGNALS.getOrDefault(plan, 0);
    }

    // OFFERS
    public boolean canSendOffer(Plan plan, int usedOffers) {
        int limit = PlanConfig.MAX_OFFERS.getOrDefault(plan, 0);
        return isAllowed(limit, usedOffers);
    }

    public int getOfferLimit(Plan plan) {
        return PlanConfig.MAX_OFFERS.getOrDefault(plan, 0);
    }

   

    public int getAiLimit(Plan plan) {
        return PlanConfig.MAX_AI_CALLS.getOrDefault(plan, 0);
    }

    //  WHATSAPP ACCESS
    public boolean hasWhatsappAccess(Plan plan) {
        return PlanConfig.WHATSAPP_UNLOCK.getOrDefault(plan, false);
    }


    //  PLAN INFO
    public int getPlanPrice(Plan plan) {
        return PlanConfig.PLAN_PRICE_PAISE.getOrDefault(plan, 0);
    }

    public int getPlanDurationDays(Plan plan) {
        return PlanConfig.PLAN_DURATION_DAYS.getOrDefault(plan, 0);
    }

    //  ANALYTICS ACCESS
    public boolean hasBasicAnalytics(Plan plan) {
        return plan == Plan.PRO || plan == Plan.CAPTAIN;
    }

    public boolean hasFullAnalytics(Plan plan) {
        return plan == Plan.PRO_PLUS || plan == Plan.GROWTH || plan == Plan.ANNUAL;
    }

    //  PRIORITY SUPPORT
    public boolean hasPrioritySupport(Plan plan) {
        return plan == Plan.GROWTH || plan == Plan.ANNUAL 
            || plan == Plan.CAPTAIN || plan == Plan.CAPTAIN_PRO;
    }


    //  PROFILE BADGE
    public String getProfileBadge(Plan plan) {
        switch (plan) {
            case PRO:
                return "PRO";
            case PRO_PLUS:
                return "PRO+";
            case GROWTH:
                return "VERIFIED";
            case ANNUAL:
                return "FOUNDING";
            case CAPTAIN:
                return "CAPTAIN";
            case CAPTAIN_PRO:
                return "SENIOR_CAPTAIN";
            default:
                return "NONE";
        }
    }

    //  HELPER: PLAN TYPE
  
    public boolean isPaidPlan(Plan plan) {
        return plan != Plan.EXPLORER;
    }

    public boolean isCaptainPlan(Plan plan) {
        return plan == Plan.CAPTAIN || plan == Plan.CAPTAIN_PRO;
    }

    public boolean isProOrAbove(Plan plan) {
        return plan == Plan.PRO || plan == Plan.PRO_PLUS 
            || plan == Plan.GROWTH || plan == Plan.ANNUAL;
    }

    public boolean canUseAI(Plan plan, int usedToday) {
    int limit = PlanConfig.MAX_AI_CALLS.getOrDefault(plan, 0);
    return isAllowed(limit, usedToday);
}

    public boolean isWhatsappUnlocked(Plan plan) {
    return PlanConfig.WHATSAPP_UNLOCK.getOrDefault(plan, false);
}

public Plan parsePlan(String planStr) {
    try {
        return Plan.valueOf(planStr.toUpperCase());
    } catch (Exception e) {
        return Plan.EXPLORER;
    }
}


}