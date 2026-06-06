package com.flossk.tts.controller;

import com.flossk.tts.dto.SpeakRequest;
import com.flossk.tts.entity.ApiKeyUsageHistory;
import com.flossk.tts.entity.GenerationLog;
import com.flossk.tts.entity.UserApiKey;
import com.flossk.tts.repository.ApiKeyUsageHistoryRepository;
import com.flossk.tts.repository.GenerationLogRepository;
import com.flossk.tts.repository.UserApiKeyRepository;
import com.flossk.tts.service.TokenCountService;
import com.flossk.tts.service.TokenLimitService;
import com.flossk.tts.service.TtsService;
import com.flossk.tts.service.VoiceManagerService;
import com.flossk.tts.util.IpAddressUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/tts")
@SecurityRequirement(name = "ApiKeyAuth")
public class TtsController {
    
    private static final Logger logger = LoggerFactory.getLogger(TtsController.class);
    private final TtsService ttsService;
    private final VoiceManagerService voiceManagerService;
    private final GenerationLogRepository generationLogRepository;
    private final ApiKeyUsageHistoryRepository usageHistoryRepository;
    private final UserApiKeyRepository apiKeyRepository;
    private final TokenCountService tokenCountService;
    private final TokenLimitService tokenLimitService;
    
    public TtsController(TtsService ttsService, VoiceManagerService voiceManagerService,
                        GenerationLogRepository generationLogRepository,
                        ApiKeyUsageHistoryRepository usageHistoryRepository,
                        UserApiKeyRepository apiKeyRepository,
                        TokenCountService tokenCountService,
                        TokenLimitService tokenLimitService) {
        this.ttsService = ttsService;
        this.voiceManagerService = voiceManagerService;
        this.generationLogRepository = generationLogRepository;
        this.usageHistoryRepository = usageHistoryRepository;
        this.apiKeyRepository = apiKeyRepository;
        this.tokenCountService = tokenCountService;
        this.tokenLimitService = tokenLimitService;
    }
    
    @Operation(
        summary = "Generate speech from text",
        description = "Converts text to speech audio using the specified voice model. Returns a WAV audio file. Requires X-API-KEY header for authentication. Available voices: edon, arta, arben, dren"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully generated audio",
            content = @Content(mediaType = "audio/wav")),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid API key"),
        @ApiResponse(responseCode = "403", description = "Forbidden - API key has no remaining tokens"),
        @ApiResponse(responseCode = "404", description = "Voice model not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/speak")
    public ResponseEntity<byte[]> speak(
            @Valid @RequestBody SpeakRequest requestBody,
            HttpServletRequest request) {
        
        try {
            String text = requestBody.getText();
            String voiceId = requestBody.getVoiceId();
            
            if (text == null || text.isBlank()) {
                return ResponseEntity.badRequest().build();
            }
            
            if (voiceId == null || voiceId.isBlank()) {
                return ResponseEntity.badRequest().build();
            }
            
            if (!voiceManagerService.hasVoice(voiceId)) {
                return ResponseEntity.notFound().build();
            }
            
            // Normalize text before TTS
            String normalizedText = ttsService.normalizeText(text);
            
            UserApiKey userApiKey = (UserApiKey) request.getAttribute("userApiKey");
            
            if (userApiKey != null) {
                // Check and reset period if needed (for monthly/yearly limits)
                tokenLimitService.checkAndResetPeriod(userApiKey);
                // Reload from database to get updated values
                userApiKey = apiKeyRepository.findById(userApiKey.getId()).orElse(userApiKey);
                
                // Check with full token count first (worst case scenario)
                int fullTokensRequired = tokenCountService.countTokens(normalizedText);
                if (!userApiKey.hasEnoughTokens(fullTokensRequired)) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
                }
            }
            
            TtsService.GenerationResult result = ttsService.generateSpeech(text, voiceId);
            
            byte[] audioData = result.getAudioData();
            if (audioData == null || audioData.length == 0) {
                logger.error("Generated audio is empty for text: '{}', voiceId: {}", text, voiceId);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
            
            // Calculate tokens: 30% if cached, 100% if not cached
            String apiKeyOwner = userApiKey != null ? userApiKey.getOwnerName() : "unknown";
            int fullTokens = tokenCountService.countTokens(normalizedText);
            boolean isCached = result.isCached();
            int tokensUsed = isCached ? (int) Math.ceil(fullTokens * 0.3) : fullTokens;
            
            if (userApiKey != null) {
                // Check tokens again with the actual amount (cached vs non-cached)
                if (!userApiKey.hasEnoughTokens(tokensUsed)) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
                }
                userApiKey.decrementTokens(tokensUsed);
                apiKeyRepository.save(userApiKey);
            }
            
            GenerationLog log = new GenerationLog(
                voiceId,
                result.getTextHash(),
                tokensUsed,
                isCached,
                apiKeyOwner
            );
            generationLogRepository.save(log);
            
            if (userApiKey != null) {
                ApiKeyUsageHistory usageHistory = new ApiKeyUsageHistory();
                usageHistory.setApiKeyId(userApiKey.getId());
                usageHistory.setApiKeyOwner(userApiKey.getOwnerName());
                usageHistory.setEndpoint("/api/tts/speak");
                usageHistory.setRequestMethod("POST");
                usageHistory.setVoiceId(voiceId);
                usageHistory.setTextHash(result.getTextHash());
                usageHistory.setTokensUsed(tokensUsed);
                usageHistory.setCached(isCached);
                usageHistory.setUserAgent(request.getHeader("User-Agent"));
                usageHistory.setIpAddress(IpAddressUtil.getClientIpAddress(request));
                usageHistoryRepository.save(usageHistory);
            }
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("audio/wav"));
            headers.setContentLength(audioData.length);
            headers.setContentDispositionFormData("attachment", "speech_" + voiceId + "_" + System.currentTimeMillis() + ".wav");
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(audioData);
                    
        } catch (IllegalArgumentException e) {
            logger.error("Invalid request parameters", e);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error generating speech", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
}
