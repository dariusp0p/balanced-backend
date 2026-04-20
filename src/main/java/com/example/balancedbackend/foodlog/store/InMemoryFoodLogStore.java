package com.example.balancedbackend.foodlog.store;

import com.example.balancedbackend.foodlog.model.FoodLog;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class InMemoryFoodLogStore {

    private final AtomicLong idSequence = new AtomicLong(1);
    private final ConcurrentHashMap<Long, FoodLog> logsById = new ConcurrentHashMap<>();

    public FoodLog create(FoodLog draft) {
        long id = idSequence.getAndIncrement();
        FoodLog saved = new FoodLog(
                id,
                draft.userId(),
                draft.logGroupId(),
                draft.name(),
                draft.date(),
                draft.time(),
                draft.calories(),
                draft.protein(),
                draft.carbs(),
                draft.fats()
        );
        logsById.put(id, saved);
        return saved;
    }

    public Optional<FoodLog> findById(long id) {
        return Optional.ofNullable(logsById.get(id));
    }

    public FoodLog save(FoodLog foodLog) {
        logsById.put(foodLog.id(), foodLog);
        return foodLog;
    }

    public void delete(long id) {
        logsById.remove(id);
    }

    public void deleteAllByUserIdAndLogGroupId(long userId, long logGroupId) {
        logsById.values().removeIf(log -> log.userId() == userId && Long.valueOf(logGroupId).equals(log.logGroupId()));
    }

    public List<FoodLog> findAllByUserId(long userId) {
        List<FoodLog> result = new ArrayList<>();
        for (FoodLog foodLog : logsById.values()) {
            if (foodLog.userId() == userId) {
                result.add(foodLog);
            }
        }
        return result;
    }
}

