package com.flossk.tts.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_api_keys")
public class UserApiKey {
    
    public enum TokenLimitType {
        UNLIMITED,      // No limit
        TOTAL,          // Total tokens (current behavior)
        MONTHLY,        // Tokens per month
        YEARLY,         // Tokens per year
        ONCE            // One-time use only
    }
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "api_key", nullable = false, unique = true)
    private UUID key;
    
    @Column(nullable = false)
    private String ownerName;
    
    @Column
    private Integer remainingTokens;
    
    @Column(name = "referer_domain")
    private String refererDomain;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "token_limit_type")
    private TokenLimitType tokenLimitType = TokenLimitType.UNLIMITED;
    
    @Column(name = "token_limit_value")
    private Integer tokenLimitValue;
    
    @Column(name = "tokens_used_this_period")
    private Integer tokensUsedThisPeriod = 0;
    
    @Column(name = "period_start_date")
    private LocalDateTime periodStartDate;
    
    @Column(name = "one_time_used")
    private Boolean oneTimeUsed = false;
    
    public UserApiKey() {
    }
    
    public UserApiKey(String ownerName, Integer remainingTokens) {
        this.key = UUID.randomUUID();
        this.ownerName = ownerName;
        this.remainingTokens = remainingTokens;
    }
    
    public UserApiKey(String ownerName, Integer remainingTokens, String refererDomain) {
        this.key = UUID.randomUUID();
        this.ownerName = ownerName;
        this.remainingTokens = remainingTokens;
        this.refererDomain = refererDomain;
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public UUID getKey() {
        return key;
    }
    
    public void setKey(UUID key) {
        this.key = key;
    }
    
    public String getOwnerName() {
        return ownerName;
    }
    
    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }
    
    public Integer getRemainingTokens() {
        return remainingTokens;
    }
    
    public void setRemainingTokens(Integer remainingTokens) {
        this.remainingTokens = remainingTokens;
    }
    
    public String getRefererDomain() {
        return refererDomain;
    }
    
    public void setRefererDomain(String refererDomain) {
        this.refererDomain = refererDomain;
    }
    
    public TokenLimitType getTokenLimitType() {
        return tokenLimitType != null ? tokenLimitType : TokenLimitType.UNLIMITED;
    }
    
    public void setTokenLimitType(TokenLimitType tokenLimitType) {
        this.tokenLimitType = tokenLimitType;
    }
    
    public Integer getTokenLimitValue() {
        return tokenLimitValue;
    }
    
    public void setTokenLimitValue(Integer tokenLimitValue) {
        this.tokenLimitValue = tokenLimitValue;
    }
    
    public Integer getTokensUsedThisPeriod() {
        return tokensUsedThisPeriod != null ? tokensUsedThisPeriod : 0;
    }
    
    public void setTokensUsedThisPeriod(Integer tokensUsedThisPeriod) {
        this.tokensUsedThisPeriod = tokensUsedThisPeriod;
    }
    
    public LocalDateTime getPeriodStartDate() {
        return periodStartDate;
    }
    
    public void setPeriodStartDate(LocalDateTime periodStartDate) {
        this.periodStartDate = periodStartDate;
    }
    
    public Boolean getOneTimeUsed() {
        return oneTimeUsed != null ? oneTimeUsed : false;
    }
    
    public void setOneTimeUsed(Boolean oneTimeUsed) {
        this.oneTimeUsed = oneTimeUsed;
    }
    
    public boolean isUnlimited() {
        return tokenLimitType == TokenLimitType.UNLIMITED || 
               (tokenLimitType == TokenLimitType.TOTAL && remainingTokens == null);
    }
    
    public boolean hasTokens() {
        if (isUnlimited()) {
            return true;
        }
        
        if (tokenLimitType == TokenLimitType.ONCE) {
            return !getOneTimeUsed();
        }
        
        if (tokenLimitType == TokenLimitType.MONTHLY || tokenLimitType == TokenLimitType.YEARLY) {
            return getTokensUsedThisPeriod() < getTokenLimitValue();
        }
        
        // TOTAL type (legacy)
        return remainingTokens != null && remainingTokens > 0;
    }
    
    public boolean hasEnoughTokens(int requiredTokens) {
        if (isUnlimited()) {
            return true;
        }
        
        if (tokenLimitType == TokenLimitType.ONCE) {
            return !getOneTimeUsed() && requiredTokens <= getTokenLimitValue();
        }
        
        if (tokenLimitType == TokenLimitType.MONTHLY || tokenLimitType == TokenLimitType.YEARLY) {
            return (getTokensUsedThisPeriod() + requiredTokens) <= getTokenLimitValue();
        }
        
        // TOTAL type (legacy)
        return remainingTokens != null && remainingTokens >= requiredTokens;
    }
    
    public void decrementTokens(int count) {
        if (isUnlimited()) {
            return;
        }
        
        if (tokenLimitType == TokenLimitType.ONCE) {
            setOneTimeUsed(true);
            return;
        }
        
        if (tokenLimitType == TokenLimitType.MONTHLY || tokenLimitType == TokenLimitType.YEARLY) {
            setTokensUsedThisPeriod(getTokensUsedThisPeriod() + count);
            return;
        }
        
        // TOTAL type (legacy)
        if (remainingTokens != null && count > 0) {
            remainingTokens = Math.max(0, remainingTokens - count);
        }
    }
    
    public void decrementTokens() {
        decrementTokens(1);
    }
    
    public int getEffectiveRemainingTokens() {
        if (isUnlimited()) {
            return Integer.MAX_VALUE;
        }
        
        if (tokenLimitType == TokenLimitType.ONCE) {
            return getOneTimeUsed() ? 0 : getTokenLimitValue();
        }
        
        if (tokenLimitType == TokenLimitType.MONTHLY || tokenLimitType == TokenLimitType.YEARLY) {
            return Math.max(0, getTokenLimitValue() - getTokensUsedThisPeriod());
        }
        
        // TOTAL type (legacy)
        return remainingTokens != null ? remainingTokens : Integer.MAX_VALUE;
    }
}
