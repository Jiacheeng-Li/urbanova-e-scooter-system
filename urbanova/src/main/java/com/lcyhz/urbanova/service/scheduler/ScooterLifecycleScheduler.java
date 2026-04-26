package com.lcyhz.urbanova.service.scheduler;

import com.lcyhz.urbanova.service.ScooterService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ScooterLifecycleScheduler {
    private final ScooterService scooterService;

    @Scheduled(fixedDelayString = "${app.scooter.lifecycle-tick-ms:60000}")
    public void tick() {
        scooterService.processScooterLifecycle();
    }
}
