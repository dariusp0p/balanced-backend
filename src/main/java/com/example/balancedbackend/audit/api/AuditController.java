package com.example.balancedbackend.audit.api;

import com.example.balancedbackend.audit.api.dto.AppActionLogResponse;
import com.example.balancedbackend.audit.api.dto.ObservedUserResponse;
import com.example.balancedbackend.audit.service.AuditService;
import com.example.balancedbackend.shared.security.SecuritySupport;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AuditController {
    private final AuditService auditService;
    private final SecuritySupport securitySupport;

    public AuditController(AuditService auditService, SecuritySupport securitySupport) {
        this.auditService = auditService;
        this.securitySupport = securitySupport;
    }

    @GetMapping("/logs")
    public List<AppActionLogResponse> logs(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size
    ) {
        securitySupport.requireAdmin(authentication);
        return auditService.getLogs(page, size);
    }

    @GetMapping("/observed-users")
    public List<ObservedUserResponse> observedUsers(Authentication authentication) {
        securitySupport.requireAdmin(authentication);
        return auditService.getObservedUsers();
    }
}
