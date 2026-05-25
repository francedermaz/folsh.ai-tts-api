package com.flossk.tts.service;

import com.flossk.tts.entity.UserApiKey;
import com.flossk.tts.repository.UserApiKeyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TokenLimitService {
    
    private static final Logger logger = LoggerFactory.getLogger(TokenLimitService.class);
    private final UserApiKeyRepository apiKeyRepository;
    
    public TokenLimitService(UserApiKeyRepository apiKeyRepository) {
        this.apiKeyRepository = apiKeyRepository;
    }
    
    /**
     * Checks if a period reset is needed and resets if necessary.
     * Called before checking token availability.
     */
    @Transactional
    public void checkAndResetPeriod(UserApiKey apiKey) {
        if (apiKey.getTokenLimitType() == UserApiKey.TokenLimitType.MONTHLY) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime periodStart = apiKey.getPeriodStartDate();
            
            if (periodStart == null || 
                now.getYear() != periodStart.getYear() || 
                now.getMonthValue() != periodStart.getMonthValue()) {
                // Reset monthly period
                apiKey.setTokensUsedThisPeriod(0);
                apiKey.setPeriodStartDate(now);
                apiKeyRepository.save(apiKey);
                logger.debug("Reset monthly tokens for API key {}", apiKey.getId());
            }
        } else if (apiKey.getTokenLimitType() == UserApiKey.TokenLimitType.YEARLY) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime periodStart = apiKey.getPeriodStartDate();
            
            if (periodStart == null || now.getYear() != periodStart.getYear()) {
                // Reset yearly period
                apiKey.setTokensUsedThisPeriod(0);
                apiKey.setPeriodStartDate(now);
                apiKeyRepository.save(apiKey);
                logger.debug("Reset yearly tokens for API key {}", apiKey.getId());
            }
        }
    }
    
    /**
     * Scheduled task to reset monthly and yearly periods.
     * Runs daily at midnight.
     */
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void resetPeriods() {
        logger.info("Running scheduled period reset task");
        List<UserApiKey> apiKeys = apiKeyRepository.findAll();
        LocalDateTime now = LocalDateTime.now();
        int resetCount = 0;
        
        for (UserApiKey apiKey : apiKeys) {
            boolean needsReset = false;
            
            if (apiKey.getTokenLimitType() == UserApiKey.TokenLimitType.MONTHLY) {
                LocalDateTime periodStart = apiKey.getPeriodStartDate();
                if (periodStart == null || 
                    now.getYear() != periodStart.getYear() || 
                    now.getMonthValue() != periodStart.getMonthValue()) {
                    needsReset = true;
                }
            } else if (apiKey.getTokenLimitType() == UserApiKey.TokenLimitType.YEARLY) {
                LocalDateTime periodStart = apiKey.getPeriodStartDate();
                if (periodStart == null || now.getYear() != periodStart.getYear()) {
                    needsReset = true;
                }
            }
            
            if (needsReset) {
                apiKey.setTokensUsedThisPeriod(0);
                apiKey.setPeriodStartDate(now);
                apiKeyRepository.save(apiKey);
                resetCount++;
            }
        }
        
        if (resetCount > 0) {
            logger.info("Reset {} API key periods", resetCount);
        }
    }
}
