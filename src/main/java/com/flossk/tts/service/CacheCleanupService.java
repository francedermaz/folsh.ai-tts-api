package com.flossk.tts.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class CacheCleanupService {
    
    private static final Logger logger = LoggerFactory.getLogger(CacheCleanupService.class);
    private final TtsService ttsService;
    
    public CacheCleanupService(TtsService ttsService) {
        this.ttsService = ttsService;
    }
    
    @Scheduled(cron = "0 0 0 * * *")
    public void cleanupOldCacheFiles() {
        logger.info("Starting cache cleanup task...");
        
        try {
            Path cacheDir = ttsService.getCacheDirectory();
            if (!Files.exists(cacheDir)) {
                logger.info("Cache directory does not exist, skipping cleanup");
                return;
            }
            
            Instant threeDaysAgo = Instant.now().minus(3, ChronoUnit.DAYS);
            final int[] deletedCount = {0};
            
            Files.list(cacheDir)
                .filter(path -> path.toString().endsWith(".wav"))
                .forEach(path -> {
                    try {
                        FileTime lastModified = Files.getLastModifiedTime(path);
                        if (lastModified.toInstant().isBefore(threeDaysAgo)) {
                            Files.delete(path);
                            deletedCount[0]++;
                            logger.debug("Deleted old cache file: {}", path.getFileName());
                        }
                    } catch (IOException e) {
                        logger.error("Error deleting cache file: {}", path, e);
                    }
                });
            
            logger.info("Cache cleanup completed. Deleted {} files", deletedCount[0]);
            
        } catch (IOException e) {
            logger.error("Error during cache cleanup", e);
        }
    }
}
