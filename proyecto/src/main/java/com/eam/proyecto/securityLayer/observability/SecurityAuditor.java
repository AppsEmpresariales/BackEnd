package com.eam.proyecto.securityLayer.observability;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SecurityAuditor {

    @EventListener
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        String username = event.getAuthentication().getName();
        log.info("SECURITY AUDIT [SUCCESS]: User '{}' successfully logged in.", username);
    }

    @EventListener
    public void onAuthenticationFailure(AbstractAuthenticationFailureEvent event) {
        String username = event.getAuthentication().getName();
        String errorMessage = event.getException().getMessage();
        log.warn("SECURITY AUDIT [FAILURE]: Failed login attempt for user '{}'. Reason: {}", username, errorMessage);
    }
}
