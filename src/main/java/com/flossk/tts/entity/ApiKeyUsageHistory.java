package com.flossk.tts.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "api_key_usage_history")
public class ApiKeyUsageHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private Long apiKeyId;
    
    @Column(nullable = false)
    private String apiKeyOwner;
    
    @Column(nullable = false)
    private String endpoint;
    
    @Column
    private String requestMethod;
    
    @Column
    private String voiceId;
    
    @Column
    private String textHash;
    
    @Column(nullable = false)
    private Integer tokensUsed;
    
    @Column(nullable = false)
    private LocalDateTime timestamp;
    
    @Column(length = 500)
    private String userAgent;
    
    @Column(length = 45)
    private String ipAddress;
    
    @Column(nullable = false)
    private Boolean cached = false;
    
    @PrePersist
    protected void onCreate() {
        timestamp = LocalDateTime.now();
    }
    
    public ApiKeyUsageHistory() {
    }
    
    public ApiKeyUsageHistory(Long apiKeyId, String apiKeyOwner, String endpoint, 
                              String requestMethod, Integer tokensUsed) {
        this.apiKeyId = apiKeyId;
        this.apiKeyOwner = apiKeyOwner;
        this.endpoint = endpoint;
        this.requestMethod = requestMethod;
        this.tokensUsed = tokensUsed;
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getApiKeyId() {
        return apiKeyId;
    }
    
    public void setApiKeyId(Long apiKeyId) {
        this.apiKeyId = apiKeyId;
    }
    
    public String getApiKeyOwner() {
        return apiKeyOwner;
    }
    
    public void setApiKeyOwner(String apiKeyOwner) {
        this.apiKeyOwner = apiKeyOwner;
    }
    
    public String getEndpoint() {
        return endpoint;
    }
    
    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }
    
    public String getRequestMethod() {
        return requestMethod;
    }
    
    public void setRequestMethod(String requestMethod) {
        this.requestMethod = requestMethod;
    }
    
    public String getVoiceId() {
        return voiceId;
    }
    
    public void setVoiceId(String voiceId) {
        this.voiceId = voiceId;
    }
    
    public String getTextHash() {
        return textHash;
    }
    
    public void setTextHash(String textHash) {
        this.textHash = textHash;
    }
    
    public Integer getTokensUsed() {
        return tokensUsed;
    }
    
    public void setTokensUsed(Integer tokensUsed) {
        this.tokensUsed = tokensUsed;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getUserAgent() {
        return userAgent;
    }
    
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }
    
    public String getIpAddress() {
        return ipAddress;
    }
    
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
    
    public Boolean getCached() {
        return cached != null ? cached : false;
    }
    
    public void setCached(Boolean cached) {
        this.cached = cached != null ? cached : false;
    }
}
