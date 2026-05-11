package com.example.balancedbackend.audit.store;

import com.example.balancedbackend.audit.model.AppActionLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppActionLogRepository extends JpaRepository<AppActionLog, Long> {
    Page<AppActionLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
