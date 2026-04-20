package com.example.balancedbackend.loggroup.store;

import com.example.balancedbackend.loggroup.model.LogGroup;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class InMemoryLogGroupStore {

    private final AtomicLong idSequence = new AtomicLong(1);
    private final ConcurrentHashMap<Long, LogGroup> groupsById = new ConcurrentHashMap<>();

    public LogGroup create(LogGroup draft) {
        long id = idSequence.getAndIncrement();
        LogGroup saved = new LogGroup(
                id,
                draft.userId(),
                draft.name(),
                draft.date(),
                draft.computeFromFoodLogs(),
                draft.totalCalories(),
                draft.totalProtein(),
                draft.totalCarbs(),
                draft.totalFats()
        );
        groupsById.put(id, saved);
        return saved;
    }

    public Optional<LogGroup> findById(long id) {
        return Optional.ofNullable(groupsById.get(id));
    }

    public LogGroup save(LogGroup group) {
        groupsById.put(group.id(), group);
        return group;
    }

    public void delete(long id) {
        groupsById.remove(id);
    }

    public List<LogGroup> findAllByUserId(long userId) {
        List<LogGroup> result = new ArrayList<>();
        for (LogGroup group : groupsById.values()) {
            if (group.userId() == userId) {
                result.add(group);
            }
        }
        return result;
    }

    public boolean isEmpty() {
        return groupsById.isEmpty();
    }
}

