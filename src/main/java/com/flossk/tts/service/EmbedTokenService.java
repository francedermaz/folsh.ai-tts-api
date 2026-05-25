package com.flossk.tts.service;

import com.flossk.tts.entity.UserApiKey;
import com.flossk.tts.repository.UserApiKeyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
public class EmbedTokenService {
    
    private static final Logger logger = LoggerFactory.getLogger(EmbedTokenService.class);
    private static final String HMAC_SHA256_ALGORITHM = "HmacSHA256";
    private final UserApiKeyRepository apiKeyRepository;
    
    public EmbedTokenService(UserApiKeyRepository apiKeyRepository) {
        this.apiKeyRepository = apiKeyRepository;
    }
    
    public String generateEmbedToken(Long apiKeyId, String domain) {
        try {
            UserApiKey apiKey = apiKeyRepository.findById(apiKeyId).orElse(null);
            if (apiKey == null) {
                throw new IllegalArgumentException("API key not found");
            }
            
            String payload = apiKeyId + "|" + domain + "|" + System.currentTimeMillis();
            String secret = apiKey.getKey().toString();
            
            Mac mac = Mac.getInstance(HMAC_SHA256_ALGORITHM);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256_ALGORITHM);
            mac.init(secretKeySpec);
            
            byte[] hmacBytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String signature = Base64.getUrlEncoder().withoutPadding().encodeToString(hmacBytes);
            
            String token = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8)) + "." + signature;
            
            return token;
        } catch (Exception e) {
            logger.error("Error generating embed token", e);
            throw new RuntimeException("Failed to generate embed token", e);
        }
    }
    
    public EmbedTokenData validateEmbedToken(String token, String refererDomain) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 2) {
                return null;
            }
            
            String payloadBase64 = parts[0];
            String signature = parts[1];
            
            byte[] payloadBytes = Base64.getUrlDecoder().decode(payloadBase64);
            String payload = new String(payloadBytes, StandardCharsets.UTF_8);
            
            String[] payloadParts = payload.split("\\|");
            if (payloadParts.length != 3) {
                return null;
            }
            
            Long apiKeyId = Long.parseLong(payloadParts[0]);
            String registeredDomain = payloadParts[1];
            
            UserApiKey apiKey = apiKeyRepository.findById(apiKeyId).orElse(null);
            if (apiKey == null) {
                return null;
            }
            
            if (apiKey.getRefererDomain() != null && !apiKey.getRefererDomain().isBlank()) {
                if (!isValidDomain(refererDomain, apiKey.getRefererDomain())) {
                    return null;
                }
            }
            
            String secret = apiKey.getKey().toString();
            Mac mac = Mac.getInstance(HMAC_SHA256_ALGORITHM);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256_ALGORITHM);
            mac.init(secretKeySpec);
            
            byte[] expectedHmacBytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String expectedSignature = Base64.getUrlEncoder().withoutPadding().encodeToString(expectedHmacBytes);
            
            if (!signature.equals(expectedSignature)) {
                return null;
            }
            
            return new EmbedTokenData(apiKeyId, registeredDomain, apiKey);
        } catch (Exception e) {
            logger.error("Error validating embed token", e);
            return null;
        }
    }
    
    private boolean isValidDomain(String refererDomain, String allowedDomain) {
        if (refererDomain == null || allowedDomain == null) {
            return false;
        }
        
        try {
            java.net.URL refererUrl = new java.net.URL(refererDomain);
            String refererHost = refererUrl.getHost().toLowerCase();
            String allowedHost = allowedDomain.toLowerCase().replaceAll("^https?://", "").replaceAll("/.*$", "");
            
            return refererHost.equals(allowedHost) || refererHost.endsWith("." + allowedHost);
        } catch (Exception e) {
            logger.warn("Error validating domain: referer={}, allowed={}", refererDomain, allowedDomain, e);
            return false;
        }
    }
    
    public static class EmbedTokenData {
        private final Long apiKeyId;
        private final String domain;
        private final UserApiKey apiKey;
        
        public EmbedTokenData(Long apiKeyId, String domain, UserApiKey apiKey) {
            this.apiKeyId = apiKeyId;
            this.domain = domain;
            this.apiKey = apiKey;
        }
        
        public Long getApiKeyId() {
            return apiKeyId;
        }
        
        public String getDomain() {
            return domain;
        }
        
        public UserApiKey getApiKey() {
            return apiKey;
        }
    }
}
