package com.flossk.tts.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "generation_logs")
public class GenerationLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String voiceId;
    
    @Column(nullable = false, length = 500)
    private String textHash;
    
    @Column(nullable = false)
    private Integer tokensUsed;
    
    @Column(nullable = false)
    private Boolean cached;
    
    @Column
    private String apiKeyOwner;
    
    @Column(nullable = false)
    private LocalDateTime timestamp;
    
    @PrePersist
    protected void onCreate() {
        timestamp = LocalDateTime.now();
    }
    
    public GenerationLog() {
    }
    
    public GenerationLog(String voiceId, String textHash, Integer tokensUsed, Boolean cached, String apiKeyOwner) {
        this.voiceId = voiceId;
        this.textHash = textHash;
        this.tokensUsed = tokensUsed;
        this.cached = cached;
        this.apiKeyOwner = apiKeyOwner;
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
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
    
    public Boolean getCached() {
        return cached;
    }
    
    public void setCached(Boolean cached) {
        this.cached = cached;
    }
    
    public String getApiKeyOwner() {
        return apiKeyOwner;
    }
    
    public void setApiKeyOwner(String apiKeyOwner) {
        this.apiKeyOwner = apiKeyOwner;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
