package com.flossk.tts.service;

import com.flossk.tts.entity.AdminUser;
import com.flossk.tts.repository.AdminUserRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class DataInitializationService {
    
    private static final Logger logger = LoggerFactory.getLogger(DataInitializationService.class);
    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;
    
    public DataInitializationService(AdminUserRepository adminUserRepository, PasswordEncoder passwordEncoder) {
        this.adminUserRepository = adminUserRepository;
        this.passwordEncoder = passwordEncoder;
    }
    
    @PostConstruct
    public void initializeDefaultAdmin() {
        if (adminUserRepository.findByUsername("floosk").isEmpty()) {
            AdminUser adminUser = new AdminUser();
            adminUser.setUsername("floosk");
            adminUser.setPassword(passwordEncoder.encode("flosskaadmin"));
            adminUserRepository.save(adminUser);
            logger.info("Created default admin user: floosk");
        }
    }
}
