package com.example.balancedbackend.foodlog.service;

import com.example.balancedbackend.common.exception.BadRequestException;
import com.example.balancedbackend.common.exception.ConflictException;
import com.example.balancedbackend.foodlog.api.dto.FoodLogResponse;
import com.example.balancedbackend.foodlog.model.FoodLog;
import com.example.balancedbackend.foodlog.service.dto.FoodLogBatchEvent;
import com.example.balancedbackend.foodlog.store.InMemoryFoodLogStore;
import jakarta.annotation.PreDestroy;
import net.datafaker.Faker;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Service
public class FoodLogGeneratorService {

    private static final int DEFAULT_BATCH_SIZE = 5;
    private static final long DEFAULT_INTERVAL_MS = 3_000L;

    private final InMemoryFoodLogStore foodLogStore;
    private final FoodLogService foodLogService;
    private final SimpMessagingTemplate messagingTemplate;
    private final Faker faker = new Faker();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final Map<Long, GeneratorJob> jobsByUser = new ConcurrentHashMap<>();

    public FoodLogGeneratorService(InMemoryFoodLogStore foodLogStore,
                                   FoodLogService foodLogService,
                                   SimpMessagingTemplate messagingTemplate) {
        this.foodLogStore = foodLogStore;
        this.foodLogService = foodLogService;
        this.messagingTemplate = messagingTemplate;
    }

    public GeneratorStatus start(long userId, String date, Integer batchSize, Long intervalMs) {
        int resolvedBatchSize = batchSize == null ? DEFAULT_BATCH_SIZE : batchSize;
        long resolvedIntervalMs = intervalMs == null ? DEFAULT_INTERVAL_MS : intervalMs;
        LocalDate targetDate = parseDate(date);

        GeneratorJob existingJob = jobsByUser.get(userId);
        if (existingJob != null && !existingJob.future().isCancelled()) {
            throw new ConflictException("Generator is already running for this user");
        }

        ScheduledFuture<?> future = scheduler.scheduleWithFixedDelay(
                () -> generateBatch(userId, targetDate, resolvedBatchSize),
                0,
                resolvedIntervalMs,
                TimeUnit.MILLISECONDS
        );

        jobsByUser.put(userId, new GeneratorJob(future, targetDate, resolvedBatchSize, resolvedIntervalMs));
        return new GeneratorStatus(true, resolvedBatchSize, resolvedIntervalMs);
    }

    public GeneratorStatus stop(long userId) {
        GeneratorJob job = jobsByUser.remove(userId);
        if (job != null) {
            job.future().cancel(true);
        }
        int batchSize = job == null ? DEFAULT_BATCH_SIZE : job.batchSize();
        long intervalMs = job == null ? DEFAULT_INTERVAL_MS : job.intervalMs();
        return new GeneratorStatus(false, batchSize, intervalMs);
    }

    public GeneratorStatus status(long userId) {
        GeneratorJob job = jobsByUser.get(userId);
        if (job == null || job.future().isCancelled()) {
            return new GeneratorStatus(false, DEFAULT_BATCH_SIZE, DEFAULT_INTERVAL_MS);
        }
        return new GeneratorStatus(true, job.batchSize(), job.intervalMs());
    }

    private void generateBatch(long userId, LocalDate targetDate, int batchSize) {
        List<FoodLogResponse> created = new ArrayList<>();

        for (int i = 0; i < batchSize; i++) {
            FoodLog draft = new FoodLog(
                    0,
                    userId,
                    null,
                    faker.options().option(
                            "Chicken Rice Bowl",
                            "Greek Yogurt Snack",
                            "Protein Oatmeal",
                            "Salmon Salad",
                            "Turkey Sandwich",
                            "Tofu Stir Fry",
                            "Egg Wrap"
                    ),
                    targetDate,
                    LocalTime.of(
                            faker.number().numberBetween(0, 24),
                            faker.number().numberBetween(0, 60)
                    ),
                    faker.number().numberBetween(120, 900),
                    faker.number().numberBetween(8, 65),
                    faker.number().numberBetween(10, 95),
                    faker.number().numberBetween(3, 40)
            );

            FoodLog saved = foodLogStore.create(draft);
            created.add(toResponse(saved));
        }

        created = created.stream()
                .sorted(Comparator.comparing(FoodLogResponse::time).reversed())
                .toList();

        FoodLogBatchEvent event = new FoodLogBatchEvent(
                userId,
                created.size(),
                Instant.now(),
                created,
                foodLogService.getStats(userId)
        );

        messagingTemplate.convertAndSend("/topic/food-logs/" + userId, event);
    }

    private LocalDate parseDate(String date) {
        try {
            return LocalDate.parse(date);
        } catch (DateTimeParseException ex) {
            throw new BadRequestException("date must be in YYYY-MM-DD format");
        }
    }

    private FoodLogResponse toResponse(FoodLog foodLog) {
        return new FoodLogResponse(
                foodLog.id(),
                foodLog.logGroupId(),
                foodLog.name(),
                foodLog.date().toString(),
                foodLog.time().toString(),
                round2(foodLog.calories()),
                round2(foodLog.protein()),
                round2(foodLog.carbs()),
                round2(foodLog.fats())
        );
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    @PreDestroy
    void shutdown() {
        jobsByUser.values().forEach(job -> job.future().cancel(true));
        scheduler.shutdownNow();
    }

    private record GeneratorJob(
            ScheduledFuture<?> future,
            LocalDate targetDate,
            int batchSize,
            long intervalMs
    ) {
    }

    public record GeneratorStatus(
            boolean running,
            int batchSize,
            long intervalMs
    ) {
    }
}
