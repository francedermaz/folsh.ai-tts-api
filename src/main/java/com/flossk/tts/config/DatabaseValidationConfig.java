package com.flossk.tts.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;

@Configuration
public class DatabaseValidationConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(DatabaseValidationConfig.class);
    
    @Bean
    @Profile("production")
    public CommandLineRunner validateMySQLDatabase(DataSource dataSource,
                                                   @Value("${spring.datasource.url:}") String datasourceUrl) {
        return args -> {
            if (datasourceUrl == null || datasourceUrl.isBlank() || datasourceUrl.startsWith("jdbc:h2:")) {
                logger.warn("================================================");
                logger.warn("WARNING: Using H2 database in production mode!");
                logger.warn("================================================");
                logger.warn("H2 is NOT recommended for production use.");
                logger.warn("Consider migrating to MySQL for better reliability.");
                logger.warn("");
                logger.warn("For MySQL configuration:");
                logger.warn("  - Set MYSQL_URL environment variable");
                logger.warn("  - Or configure spring.datasource.url in application.properties");
                logger.warn("================================================");
                
                try (Connection connection = dataSource.getConnection()) {
                    DatabaseMetaData metaData = connection.getMetaData();
                    String databaseProductName = metaData.getDatabaseProductName();
                    String databaseProductVersion = metaData.getDatabaseProductVersion();
                    
                    logger.info("H2 Database Connection Successful!");
                    logger.info("Database: {} {}", databaseProductName, databaseProductVersion);
                    logger.info("URL: {}", datasourceUrl);
                    logger.warn("Using H2 in production - ensure regular backups!");
                } catch (Exception e) {
                    logger.error("Failed to connect to database: {}", e.getMessage());
                    throw new IllegalStateException("Failed to connect to database: " + e.getMessage(), e);
                }
                return;
            }
            
            if (!datasourceUrl.startsWith("jdbc:mysql:")) {
                logger.warn("================================================");
                logger.warn("WARNING: Non-MySQL database detected in production!");
                logger.warn("Current database URL: {}", datasourceUrl);
                logger.warn("MySQL is recommended for production use.");
                logger.warn("================================================");
            }
            
            try (Connection connection = dataSource.getConnection()) {
                DatabaseMetaData metaData = connection.getMetaData();
                String databaseProductName = metaData.getDatabaseProductName();
                String databaseProductVersion = metaData.getDatabaseProductVersion();
                
                logger.info("================================================");
                logger.info("MySQL Database Connection Successful!");
                logger.info("Database: {} {}", databaseProductName, databaseProductVersion);
                logger.info("URL: {}", datasourceUrl);
                logger.info("================================================");
            } catch (Exception e) {
                logger.error("================================================");
                logger.error("ERROR: Failed to connect to database!");
                logger.error("================================================");
                logger.error("Please check your database configuration:");
                logger.error("  - Database server is running");
                logger.error("  - Connection URL is correct");
                logger.error("  - Username and password are correct");
                logger.error("  - Database exists");
                logger.error("================================================");
                throw new IllegalStateException("Failed to connect to database: " + e.getMessage(), e);
            }
        };
    }
    
    @Bean
    @Profile("!production")
    public CommandLineRunner logDatabaseInfo(DataSource dataSource,
                                            @Value("${spring.datasource.url:}") String datasourceUrl) {
        return args -> {
            try (Connection connection = dataSource.getConnection()) {
                DatabaseMetaData metaData = connection.getMetaData();
                String databaseProductName = metaData.getDatabaseProductName();
                String databaseProductVersion = metaData.getDatabaseProductVersion();
                
                logger.info("================================================");
                logger.info("Database Connection Successful!");
                logger.info("Database: {} {}", databaseProductName, databaseProductVersion);
                logger.info("URL: {}", datasourceUrl);
                if (datasourceUrl.startsWith("jdbc:h2:")) {
                    logger.info("Using H2 file-based database (development mode)");
                }
                logger.info("================================================");
            } catch (Exception e) {
                logger.warn("Could not log database info: {}", e.getMessage());
            }
        };
    }
}
