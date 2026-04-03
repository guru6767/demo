package com.starto.scheduler;

import com.starto.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Component
@RequiredArgsConstructor
public class OnlineStatusScheduler {

    private final UserRepository userRepository;

    // runs every 1 minute
    @Transactional
    @Scheduled(fixedRate = 60_000)
    public void markInactiveUsersOffline() {
        OffsetDateTime cutoff = OffsetDateTime.now().minusMinutes(5);
        userRepository.markInactiveUsersOffline(cutoff);
    }
}