package com.fitnesslife.gym.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Component;

@Component
public class SessionCleaner {

    @Autowired
    private SessionRegistry sessionRegistry;

    @PostConstruct
    public void clearSessionsOnStartup() {
        sessionRegistry.getAllPrincipals().forEach(principal -> {
            sessionRegistry.getAllSessions(principal, false)
                    .forEach(sessionInfo -> sessionInfo.expireNow());
        });
        System.out.println("Todas las sesiones activas fueron invalidadas al iniciar la aplicación.");
    }
}
