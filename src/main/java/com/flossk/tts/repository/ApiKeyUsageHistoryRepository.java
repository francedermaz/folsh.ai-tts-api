package com.flossk.tts.repository;

import com.flossk.tts.entity.ApiKeyUsageHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ApiKeyUsageHistoryRepository extends JpaRepository<ApiKeyUsageHistory, Long> {
    List<ApiKeyUsageHistory> findByApiKeyIdOrderByTimestampDesc(Long apiKeyId);
    List<ApiKeyUsageHistory> findByApiKeyOwnerOrderByTimestampDesc(String apiKeyOwner);
    Page<ApiKeyUsageHistory> findByApiKeyIdOrderByTimestampDesc(Long apiKeyId, Pageable pageable);
    
    @Query("SELECT h FROM ApiKeyUsageHistory h WHERE h.apiKeyId = :apiKeyId " +
           "AND (:voiceId IS NULL OR h.voiceId = :voiceId) " +
           "AND (:cached IS NULL OR h.cached = :cached) " +
           "AND (:endpoint IS NULL OR h.endpoint LIKE %:endpoint%) " +
           "AND (:ipAddress IS NULL OR h.ipAddress LIKE %:ipAddress%) " +
           "AND (:dateFrom IS NULL OR h.timestamp >= :dateFrom) " +
           "AND (:dateTo IS NULL OR h.timestamp <= :dateTo) " +
           "ORDER BY h.timestamp DESC")
    Page<ApiKeyUsageHistory> findByApiKeyIdWithFilters(
            @Param("apiKeyId") Long apiKeyId,
            @Param("voiceId") String voiceId,
            @Param("cached") Boolean cached,
            @Param("endpoint") String endpoint,
            @Param("ipAddress") String ipAddress,
            @Param("dateFrom") LocalDateTime dateFrom,
            @Param("dateTo") LocalDateTime dateTo,
            Pageable pageable);
    
    @Query("SELECT h FROM ApiKeyUsageHistory h WHERE h.apiKeyId = :apiKeyId " +
           "AND (:voiceId IS NULL OR h.voiceId = :voiceId) " +
           "AND (:cached IS NULL OR h.cached = :cached) " +
           "AND (:endpoint IS NULL OR h.endpoint LIKE %:endpoint%) " +
           "AND (:ipAddress IS NULL OR h.ipAddress LIKE %:ipAddress%) " +
           "AND (:dateFrom IS NULL OR h.timestamp >= :dateFrom) " +
           "AND (:dateTo IS NULL OR h.timestamp <= :dateTo) " +
           "ORDER BY h.timestamp DESC")
    List<ApiKeyUsageHistory> findByApiKeyIdWithFiltersForStats(
            @Param("apiKeyId") Long apiKeyId,
            @Param("voiceId") String voiceId,
            @Param("cached") Boolean cached,
            @Param("endpoint") String endpoint,
            @Param("ipAddress") String ipAddress,
            @Param("dateFrom") LocalDateTime dateFrom,
            @Param("dateTo") LocalDateTime dateTo);
}
