package com.flossk.tts.controller;

import com.flossk.tts.entity.ApiKeyUsageHistory;
import com.flossk.tts.entity.UserApiKey;
import com.flossk.tts.repository.ApiKeyUsageHistoryRepository;
import com.flossk.tts.repository.UserApiKeyRepository;
import com.flossk.tts.service.EmbedTokenService;
import com.flossk.tts.service.TokenCountService;
import com.flossk.tts.service.TokenLimitService;
import com.flossk.tts.service.TtsService;
import com.flossk.tts.service.VoiceManagerService;
import com.flossk.tts.util.IpAddressUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;


@Controller
@RequestMapping("/embed")
public class EmbedController {
    
    private static final Logger logger = LoggerFactory.getLogger(EmbedController.class);
    private final TtsService ttsService;
    private final VoiceManagerService voiceManagerService;
    private final UserApiKeyRepository apiKeyRepository;
    private final ApiKeyUsageHistoryRepository usageHistoryRepository;
    private final EmbedTokenService embedTokenService;
    private final TokenCountService tokenCountService;
    private final TokenLimitService tokenLimitService;
    
    public EmbedController(TtsService ttsService, VoiceManagerService voiceManagerService,
                          UserApiKeyRepository apiKeyRepository,
                          ApiKeyUsageHistoryRepository usageHistoryRepository,
                          EmbedTokenService embedTokenService,
                          TokenCountService tokenCountService,
                          TokenLimitService tokenLimitService) {
        this.ttsService = ttsService;
        this.voiceManagerService = voiceManagerService;
        this.apiKeyRepository = apiKeyRepository;
        this.usageHistoryRepository = usageHistoryRepository;
        this.embedTokenService = embedTokenService;
        this.tokenCountService = tokenCountService;
        this.tokenLimitService = tokenLimitService;
    }
    
    @GetMapping("/player")
    public String player(@RequestParam String token,
                        HttpServletRequest request,
                        Model model) {
        
        String referer = request.getHeader("Referer");
        EmbedTokenService.EmbedTokenData tokenData = embedTokenService.validateEmbedToken(token, referer);
        
        if (tokenData == null) {
            model.addAttribute("error", "Invalid or expired embed token. Please check your referer domain settings.");
            return "embed/error";
        }
        
        model.addAttribute("token", token);
        return "embed/player";
    }
    
    @GetMapping("/audio")
    public ResponseEntity<byte[]> getAudio(
            @RequestParam String token,
            @RequestParam String text,
            @RequestParam(required = false, defaultValue = "edon") String voiceId,
            HttpServletRequest request) {
        
        try {
            String referer = request.getHeader("Referer");
            EmbedTokenService.EmbedTokenData tokenData = embedTokenService.validateEmbedToken(token, referer);
            
            if (tokenData == null) {
                logger.warn("Invalid embed token or referer mismatch");
                return ResponseEntity.status(403).build();
            }
            
            UserApiKey userApiKey = tokenData.getApiKey();
            
            if (text == null || text.isBlank()) {
                return ResponseEntity.badRequest().build();
            }
            
            // Check and reset period if needed (for monthly/yearly limits)
            tokenLimitService.checkAndResetPeriod(userApiKey);
            // Reload from database to get updated values
            userApiKey = apiKeyRepository.findById(userApiKey.getId()).orElse(userApiKey);
            
            // Normalize text: replace newlines with " ."
            String normalizedText = ttsService.normalizeText(text);
            
            // Check with full token count first (worst case scenario)
            int fullTokensRequired = tokenCountService.countTokens(normalizedText);
            if (!userApiKey.hasEnoughTokens(fullTokensRequired)) {
                return ResponseEntity.status(403).build();
            }
            
            if (!voiceManagerService.hasVoice(voiceId)) {
                return ResponseEntity.notFound().build();
            }
            
            TtsService.GenerationResult result = ttsService.generateSpeech(text, voiceId);
            byte[] audioData = result.getAudioData();
            
            if (audioData == null || audioData.length == 0) {
                return ResponseEntity.status(500).build();
            }
            
            // Calculate tokens: 30% if cached, 100% if not cached
            boolean isCached = result.isCached();
            int tokensUsed = isCached ? (int) Math.ceil(fullTokensRequired * 0.3) : fullTokensRequired;
            
            userApiKey.decrementTokens(tokensUsed);
            apiKeyRepository.save(userApiKey);
            
            try {
                ApiKeyUsageHistory usageHistory = new ApiKeyUsageHistory();
                usageHistory.setApiKeyId(userApiKey.getId());
                usageHistory.setApiKeyOwner(userApiKey.getOwnerName());
                usageHistory.setEndpoint("/embed/audio");
                usageHistory.setRequestMethod("GET");
                usageHistory.setVoiceId(voiceId);
                usageHistory.setTextHash(result.getTextHash());
                usageHistory.setTokensUsed(tokensUsed);
                usageHistory.setCached(isCached);
                usageHistory.setUserAgent(request.getHeader("User-Agent"));
                usageHistory.setIpAddress(IpAddressUtil.getClientIpAddress(request));
                usageHistoryRepository.save(usageHistory);
            } catch (Exception e) {
                logger.warn("Failed to log usage history for embed", e);
            }
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("audio/wav"));
            headers.setContentLength(audioData.length);
            headers.set("Access-Control-Allow-Origin", "*");
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(audioData);
                    
        } catch (Exception e) {
            logger.error("Error generating audio for embed", e);
            return ResponseEntity.status(500).build();
        }
    }
    
    private String extractDomain(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        try {
            java.net.URL urlObj = new java.net.URL(url);
            return urlObj.getHost().toLowerCase();
        } catch (Exception e) {
            String domain = url.toLowerCase()
                .replaceAll("^https?://", "")
                .replaceAll("/.*$", "")
                .replaceAll(":.*$", "");
            return domain;
        }
    }
    
}
