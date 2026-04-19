package com.starto.config;

import java.util.Map;
import com.starto.enums.Plan;

public class PlanConfig {

    public static final Map<Plan, Integer> MAX_SIGNALS = Map.of(
            Plan.EXPLORER, 2,
            Plan.TRIAL, 5,
            Plan.SPRINT, 5,
            Plan.BOOST, 8,
            Plan.PRO, 10,
            Plan.PRO_PLUS, Integer.MAX_VALUE,
            Plan.GROWTH, Integer.MAX_VALUE,
            Plan.ANNUAL, Integer.MAX_VALUE,
            Plan.CAPTAIN, 10,
            Plan.CAPTAIN_PRO, Integer.MAX_VALUE);

    public static final Map<Plan, Integer> MAX_OFFERS = Map.of(
            Plan.EXPLORER, 3,
            Plan.TRIAL, 10,
            Plan.SPRINT, 20,
            Plan.BOOST, Integer.MAX_VALUE,
            Plan.PRO, Integer.MAX_VALUE,
            Plan.PRO_PLUS, Integer.MAX_VALUE,
            Plan.GROWTH, Integer.MAX_VALUE,
            Plan.ANNUAL, Integer.MAX_VALUE,
            Plan.CAPTAIN, Integer.MAX_VALUE,
            Plan.CAPTAIN_PRO, Integer.MAX_VALUE);

    public static final Map<Plan, Integer> MAX_AI_CALLS = Map.of(
            Plan.EXPLORER, 0,
            Plan.TRIAL, 5,
            Plan.SPRINT, 10,
            Plan.BOOST, 15,
            Plan.PRO, 20,
            Plan.PRO_PLUS, 30,
            Plan.GROWTH, Integer.MAX_VALUE,
            Plan.ANNUAL, Integer.MAX_VALUE,
            Plan.CAPTAIN, 20,
            Plan.CAPTAIN_PRO, Integer.MAX_VALUE);

    public static final Map<Plan, Boolean> WHATSAPP_UNLOCK = Map.of(
            Plan.EXPLORER, false,
            Plan.TRIAL, true,
            Plan.SPRINT, true,
            Plan.BOOST, true,
            Plan.PRO, true,
            Plan.PRO_PLUS, true,
            Plan.GROWTH, true,
            Plan.ANNUAL, true,
            Plan.CAPTAIN, true,
            Plan.CAPTAIN_PRO, true);

    public static final Map<Plan, Integer> PLAN_DURATION_DAYS = Map.of(
            Plan.EXPLORER, Integer.MAX_VALUE,
            Plan.TRIAL, 7,
            Plan.SPRINT, 7,
            Plan.BOOST, 15,
            Plan.PRO, 30,
            Plan.PRO_PLUS, 90,
            Plan.GROWTH, 180,
            Plan.ANNUAL, 365,
            Plan.CAPTAIN, 30,
            Plan.CAPTAIN_PRO, 365);

    public static final Map<Plan, Integer> PLAN_PRICE_PAISE = Map.of(
            Plan.EXPLORER, 0,
            Plan.TRIAL, 2900,
            Plan.SPRINT, 5900,
            Plan.BOOST, 9900,
            Plan.PRO, 14900,
            Plan.PRO_PLUS, 34900,
            Plan.GROWTH, 57900,
            Plan.ANNUAL, 99900,
            Plan.CAPTAIN, 9900,
            Plan.CAPTAIN_PRO, 79900);
}