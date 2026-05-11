package com.example.balancedbackend.audit.service;

import com.example.balancedbackend.audit.api.dto.AppActionLogResponse;
import com.example.balancedbackend.audit.api.dto.ObservedUserResponse;
import com.example.balancedbackend.audit.model.AppActionLog;
import com.example.balancedbackend.audit.model.ObservedUser;
import com.example.balancedbackend.audit.store.AppActionLogRepository;
import com.example.balancedbackend.audit.store.ObservedUserRepository;
import com.example.balancedbackend.auth.model.User;
import com.example.balancedbackend.auth.store.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class AuditService {
    private final AppActionLogRepository appActionLogRepository;
    private final ObservedUserRepository observedUserRepository;
    private final UserRepository userRepository;

    public AuditService(
            AppActionLogRepository appActionLogRepository,
            ObservedUserRepository observedUserRepository,
            UserRepository userRepository
    ) {
        this.appActionLogRepository = appActionLogRepository;
        this.observedUserRepository = observedUserRepository;
        this.userRepository = userRepository;
    }

    public void logAction(long userId, String actionInformation) {
        User user = userRepository.findById(userId).orElse(null);
        AppActionLog log = new AppActionLog();
        log.setUserId(userId);
        log.setGroupId(user != null && user.isAdmin() ? "ADMIN" : "USER");
        log.setActionInformation(actionInformation);
        appActionLogRepository.save(log);
    }

    @Transactional
    public void observeUser(long userId, String reason) {
        ObservedUser observedUser = observedUserRepository.findByUserId(userId)
                .orElseGet(ObservedUser::new);
        observedUser.setUserId(userId);
        observedUser.setReason(reason);
        observedUser.setObservedAt(Instant.now());
        observedUser.setActive(true);
        observedUserRepository.save(observedUser);
    }

    public List<AppActionLogResponse> getLogs(int page, int size) {
        return appActionLogRepository
                .findAllByOrderByCreatedAtDesc(PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"))))
                .getContent()
                .stream()
                .map(this::toLogResponse)
                .toList();
    }

    public List<ObservedUserResponse> getObservedUsers() {
        return observedUserRepository.findAllByActiveTrueOrderByObservedAtDesc()
                .stream()
                .map(this::toObservedResponse)
                .toList();
    }

    private AppActionLogResponse toLogResponse(AppActionLog log) {
        User user = userRepository.findById(log.getUserId()).orElse(null);
        return new AppActionLogResponse(
                log.getId(),
                log.getUserId(),
                user == null ? "Unknown user" : user.getName(),
                user == null ? "" : user.getEmail(),
                log.getGroupId(),
                log.getActionInformation(),
                log.getCreatedAt()
        );
    }

    private ObservedUserResponse toObservedResponse(ObservedUser observedUser) {
        User user = userRepository.findById(observedUser.getUserId()).orElse(null);
        return new ObservedUserResponse(
                observedUser.getId(),
                observedUser.getUserId(),
                user == null ? "Unknown user" : user.getName(),
                user == null ? "" : user.getEmail(),
                observedUser.getReason(),
                observedUser.getObservedAt()
        );
    }
}
