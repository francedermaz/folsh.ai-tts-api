package com.flossk.tts.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class DatabaseDialectConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(DatabaseDialectConfig.class);
    
    private final Environment environment;
    private final String datasourceUrl;
    
    public DatabaseDialectConfig(Environment environment,
                                @Value("${spring.datasource.url:}") String datasourceUrl) {
        this.environment = environment;
        this.datasourceUrl = datasourceUrl;
    }
    
    @PostConstruct
    public void configureDialect() {
        if (datasourceUrl != null && !datasourceUrl.isBlank()) {
            String dialect = environment.getProperty("spring.jpa.database-platform");
            
            if (datasourceUrl.startsWith("jdbc:mysql:")) {
                if (dialect == null || dialect.contains("H2")) {
                    logger.info("MySQL detected, but dialect is set to H2. Please set spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect");
                } else {
                    logger.info("Using MySQL database with dialect: {}", dialect);
                }
            } else if (datasourceUrl.startsWith("jdbc:h2:")) {
                logger.info("Using H2 file-based database");
            }
        }
    }
}
