package com.starto.scheduler;

import com.starto.model.Signal;
import com.starto.repository.SignalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class SignalExpiryScheduler {

    private final SignalRepository signalRepository;

    // SignalExpiryScheduler.java — replace with bulk DB update
@Scheduled(cron = "0 0 * * * *")
@Transactional
public void checkExpiredSignals() {
    signalRepository.expireOldSignals(OffsetDateTime.now());
}

    @Scheduled(cron = "0 0 0 * * *") // Every midnight
    @Transactional
    public void checkExpiredBoosts() {
        log.info("Checking for expired boosts...");
        OffsetDateTime now = OffsetDateTime.now();

        // This would require a specialized query in repository
        // For simplicity, we'll mark it as a task to implement further logic if needed
    }
}
