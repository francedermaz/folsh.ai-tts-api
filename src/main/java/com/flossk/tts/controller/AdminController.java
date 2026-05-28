package com.flossk.tts.controller;

import com.flossk.tts.entity.AdminUser;
import com.flossk.tts.entity.ApiKeyUsageHistory;
import com.flossk.tts.entity.UserApiKey;
import com.flossk.tts.repository.AdminUserRepository;
import com.flossk.tts.repository.ApiKeyUsageHistoryRepository;
import com.flossk.tts.repository.UserApiKeyRepository;
import com.flossk.tts.service.EmbedTokenService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/admin")
public class AdminController {
    
    private final UserApiKeyRepository apiKeyRepository;
    private final ApiKeyUsageHistoryRepository usageHistoryRepository;
    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmbedTokenService embedTokenService;
    
    public AdminController(UserApiKeyRepository apiKeyRepository,
                          ApiKeyUsageHistoryRepository usageHistoryRepository,
                          AdminUserRepository adminUserRepository,
                          PasswordEncoder passwordEncoder,
                          EmbedTokenService embedTokenService) {
        this.apiKeyRepository = apiKeyRepository;
        this.usageHistoryRepository = usageHistoryRepository;
        this.adminUserRepository = adminUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.embedTokenService = embedTokenService;
    }
    
    @GetMapping
    public String dashboard(Model model) {
        List<UserApiKey> apiKeys = apiKeyRepository.findAll();
        model.addAttribute("apiKeys", apiKeys);
        return "admin/dashboard";
    }
    
    @GetMapping("/api-keys/{id}/history")
    public String apiKeyHistory(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String voiceId,
            @RequestParam(required = false) String cached,
            @RequestParam(required = false) String endpoint,
            @RequestParam(required = false) String ipAddress,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            Model model) {
        UserApiKey apiKey = apiKeyRepository.findById(id).orElse(null);
        if (apiKey == null) {
            return "redirect:/admin";
        }
        
        // Parse filter parameters
        Boolean cachedFilter = null;
        if (cached != null && !cached.isBlank()) {
            if ("true".equalsIgnoreCase(cached) || "yes".equalsIgnoreCase(cached)) {
                cachedFilter = true;
            } else if ("false".equalsIgnoreCase(cached) || "no".equalsIgnoreCase(cached)) {
                cachedFilter = false;
            }
        }
        
        String voiceIdFilter = (voiceId != null && !voiceId.isBlank()) ? voiceId : null;
        String endpointFilter = (endpoint != null && !endpoint.isBlank()) ? endpoint : null;
        String ipAddressFilter = (ipAddress != null && !ipAddress.isBlank()) ? ipAddress : null;
        
        java.time.LocalDateTime dateFromFilter = null;
        java.time.LocalDateTime dateToFilter = null;
        try {
            if (dateFrom != null && !dateFrom.isBlank()) {
                dateFromFilter = java.time.LocalDateTime.parse(dateFrom + "T00:00:00");
            }
            if (dateTo != null && !dateTo.isBlank()) {
                dateToFilter = java.time.LocalDateTime.parse(dateTo + "T23:59:59");
            }
        } catch (Exception e) {
            // Invalid date format, ignore filter
        }
        
        Pageable pageable = PageRequest.of(page, size);
        Page<ApiKeyUsageHistory> historyPage = usageHistoryRepository.findByApiKeyIdWithFilters(
                id, voiceIdFilter, cachedFilter, endpointFilter, ipAddressFilter, dateFromFilter, dateToFilter, pageable);
        
        // Get filtered history for summary statistics
        List<ApiKeyUsageHistory> filteredHistory = usageHistoryRepository.findByApiKeyIdWithFiltersForStats(
                id, voiceIdFilter, cachedFilter, endpointFilter, ipAddressFilter, dateFromFilter, dateToFilter);
        
        // Calculate summary statistics based on filtered results
        int totalTokensUsed = filteredHistory.stream()
            .mapToInt(h -> h.getTokensUsed() != null ? h.getTokensUsed() : 0)
            .sum();
        
        long totalRequests = filteredHistory.size();
        
        // Calculate tokens used today (from filtered results)
        java.time.LocalDateTime todayStart = java.time.LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        int tokensUsedToday = filteredHistory.stream()
            .filter(h -> h.getTimestamp() != null && h.getTimestamp().isAfter(todayStart))
            .mapToInt(h -> h.getTokensUsed() != null ? h.getTokensUsed() : 0)
            .sum();
        
        // Calculate tokens used this month (from filtered results)
        java.time.LocalDateTime monthStart = java.time.LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        int tokensUsedThisMonth = filteredHistory.stream()
            .filter(h -> h.getTimestamp() != null && h.getTimestamp().isAfter(monthStart))
            .mapToInt(h -> h.getTokensUsed() != null ? h.getTokensUsed() : 0)
            .sum();
        
        model.addAttribute("apiKey", apiKey);
        model.addAttribute("history", historyPage.getContent());
        model.addAttribute("historyPage", historyPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", size);
        model.addAttribute("totalTokensUsed", totalTokensUsed);
        model.addAttribute("totalRequests", totalRequests);
        model.addAttribute("tokensUsedToday", tokensUsedToday);
        model.addAttribute("tokensUsedThisMonth", tokensUsedThisMonth);
        
        // Filter values for form
        model.addAttribute("filterVoiceId", voiceIdFilter);
        model.addAttribute("filterCached", cached);
        model.addAttribute("filterEndpoint", endpointFilter);
        model.addAttribute("filterIpAddress", ipAddressFilter);
        model.addAttribute("filterDateFrom", dateFrom);
        model.addAttribute("filterDateTo", dateTo);
        
        return "admin/api-key-history";
    }
    
    @GetMapping("/api-keys/{id}/iframe")
    public String apiKeyIframe(@PathVariable Long id, Model model, HttpServletRequest request) {
        UserApiKey apiKey = apiKeyRepository.findById(id).orElse(null);
        if (apiKey == null) {
            return "redirect:/admin";
        }
        
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();
        String baseUrl = scheme + "://" + serverName + (serverPort != 80 && serverPort != 443 ? ":" + serverPort : "");
        
        String refererDomain = apiKey.getRefererDomain();
        if (refererDomain == null || refererDomain.isBlank()) {
            refererDomain = request.getServerName();
        }
        
        String embedToken = embedTokenService.generateEmbedToken(apiKey.getId(), refererDomain);
        String iframeUrl = baseUrl + "/embed/player?token=" + embedToken;
        
        // Plug-and-play: Include auto-script that reads attributes and sends postMessage
        String exampleText = "Përshendetje, ky është zëri shqip.";
        String exampleVoice = "arta";
        String iframeId = "flossk-tts";
        String iframeCodeWithText = "<iframe id=\"" + iframeId + "\" src=\"" + iframeUrl + "\" text_to_speech=\"" + exampleText + "\" voice_id=\"" + exampleVoice + "\" width=\"500\" height=\"400\" frameborder=\"0\"></iframe>\n" +
            "<script>(function(){var i=document.getElementById('" + iframeId + "');i.onload=function(){var t=i.getAttribute('text_to_speech'),v=i.getAttribute('voice_id')||'edon';if(t)i.contentWindow.postMessage({type:'flossk-tts-play',text:t,voice_id:v},'*');};})();</script>";
        String iframeCodeWithoutText = "<iframe src=\"" + iframeUrl + "\" width=\"500\" height=\"400\" frameborder=\"0\"></iframe>";
        
        model.addAttribute("apiKey", apiKey);
        model.addAttribute("embedToken", embedToken);
        model.addAttribute("iframeUrl", iframeUrl);
        model.addAttribute("iframeCodeWithText", iframeCodeWithText);
        model.addAttribute("iframeCodeWithoutText", iframeCodeWithoutText);
        model.addAttribute("baseUrl", baseUrl);
        return "admin/api-key-iframe";
    }
    
    @GetMapping("/password")
    public String passwordChangeForm(Model model) {
        return "admin/password-change";
    }
    
    @PostMapping("/password")
    public String changePassword(
            @RequestParam String currentPassword,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        
        if (newPassword == null || newPassword.length() < 6) {
            redirectAttributes.addFlashAttribute("error", "New password must be at least 6 characters");
            return "redirect:/admin/password";
        }
        
        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "New passwords do not match");
            return "redirect:/admin/password";
        }
        
        AdminUser adminUser = adminUserRepository.findByUsername(authentication.getName())
            .orElse(null);
        
        if (adminUser == null) {
            redirectAttributes.addFlashAttribute("error", "Admin user not found");
            return "redirect:/admin/password";
        }
        
        if (!passwordEncoder.matches(currentPassword, adminUser.getPassword())) {
            redirectAttributes.addFlashAttribute("error", "Current password is incorrect");
            return "redirect:/admin/password";
        }
        
        adminUser.setPassword(passwordEncoder.encode(newPassword));
        adminUserRepository.save(adminUser);
        
        redirectAttributes.addFlashAttribute("success", "Password changed successfully");
        return "redirect:/admin";
    }
    
    @PostMapping("/api-keys")
    public String createApiKey(
            @RequestParam String ownerName,
            @RequestParam(required = false) String tokenLimitType,
            @RequestParam(required = false) Integer tokenLimitValue,
            @RequestParam(required = false) String refererDomain,
            RedirectAttributes redirectAttributes) {
        
        if (ownerName == null || ownerName.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Owner name is required");
            return "redirect:/admin";
        }
        
        UserApiKey apiKey = new UserApiKey();
        apiKey.setKey(UUID.randomUUID());
        apiKey.setOwnerName(ownerName);
        apiKey.setRefererDomain(refererDomain != null && refererDomain.isBlank() ? null : refererDomain);
        
        // Set token limit type and value
        if (tokenLimitType == null || tokenLimitType.isBlank() || "unlimited".equals(tokenLimitType)) {
            apiKey.setTokenLimitType(UserApiKey.TokenLimitType.UNLIMITED);
            apiKey.setTokenLimitValue(null);
            apiKey.setRemainingTokens(null);
        } else {
            UserApiKey.TokenLimitType limitType = UserApiKey.TokenLimitType.valueOf(tokenLimitType.toUpperCase());
            apiKey.setTokenLimitType(limitType);
            
            if (tokenLimitValue == null || tokenLimitValue < 0) {
                redirectAttributes.addFlashAttribute("error", "Token limit value must be a positive number");
                return "redirect:/admin";
            }
            
            apiKey.setTokenLimitValue(tokenLimitValue);
            
            if (limitType == UserApiKey.TokenLimitType.TOTAL) {
                // Legacy behavior: use remainingTokens
                apiKey.setRemainingTokens(tokenLimitValue);
            } else if (limitType == UserApiKey.TokenLimitType.MONTHLY || limitType == UserApiKey.TokenLimitType.YEARLY) {
                apiKey.setTokensUsedThisPeriod(0);
                apiKey.setPeriodStartDate(java.time.LocalDateTime.now());
            } else if (limitType == UserApiKey.TokenLimitType.ONCE) {
                apiKey.setOneTimeUsed(false);
            }
        }
        
        apiKeyRepository.save(apiKey);
        
        redirectAttributes.addFlashAttribute("success", 
            "API key created: " + apiKey.getKey());
        
        return "redirect:/admin";
    }
    
    @PostMapping("/api-keys/{id}/update-domain")
    public String updateRefererDomain(
            @PathVariable Long id,
            @RequestParam(required = false) String refererDomain,
            RedirectAttributes redirectAttributes) {
        
        UserApiKey apiKey = apiKeyRepository.findById(id).orElse(null);
        if (apiKey == null) {
            redirectAttributes.addFlashAttribute("error", "API key not found");
            return "redirect:/admin";
        }
        
        apiKey.setRefererDomain(refererDomain != null && refererDomain.isBlank() ? null : refererDomain);
        apiKeyRepository.save(apiKey);
        
        redirectAttributes.addFlashAttribute("success", "Referer domain updated");
        return "redirect:/admin";
    }
    
    @PostMapping("/api-keys/{id}/update-tokens")
    public String updateRemainingTokens(
            @PathVariable Long id,
            @RequestParam(required = false) Integer remainingTokens,
            RedirectAttributes redirectAttributes) {
        
        UserApiKey apiKey = apiKeyRepository.findById(id).orElse(null);
        if (apiKey == null) {
            redirectAttributes.addFlashAttribute("error", "API key not found");
            return "redirect:/admin";
        }
        
        if (remainingTokens != null && remainingTokens < 0) {
            redirectAttributes.addFlashAttribute("error", "Remaining tokens cannot be negative");
            return "redirect:/admin";
        }
        
        apiKey.setRemainingTokens(remainingTokens);
        apiKeyRepository.save(apiKey);
        
        redirectAttributes.addFlashAttribute("success", "Remaining tokens updated");
        return "redirect:/admin";
    }
    
    @PostMapping("/api-keys/{id}/update")
    public String updateApiKey(
            @PathVariable Long id,
            @RequestParam(required = false) String tokenLimitType,
            @RequestParam(required = false) Integer tokenLimitValue,
            @RequestParam(required = false) String refererDomain,
            RedirectAttributes redirectAttributes) {
        
        UserApiKey apiKey = apiKeyRepository.findById(id).orElse(null);
        if (apiKey == null) {
            redirectAttributes.addFlashAttribute("error", "API key not found");
            return "redirect:/admin";
        }
        
        apiKey.setRefererDomain(refererDomain != null && refererDomain.isBlank() ? null : refererDomain);
        
        // Update token limit type and value
        if (tokenLimitType == null || tokenLimitType.isBlank() || "unlimited".equals(tokenLimitType)) {
            apiKey.setTokenLimitType(UserApiKey.TokenLimitType.UNLIMITED);
            apiKey.setTokenLimitValue(null);
            apiKey.setRemainingTokens(null);
        } else {
            UserApiKey.TokenLimitType limitType = UserApiKey.TokenLimitType.valueOf(tokenLimitType.toUpperCase());
            apiKey.setTokenLimitType(limitType);
            
            if (tokenLimitValue == null || tokenLimitValue < 0) {
                redirectAttributes.addFlashAttribute("error", "Token limit value must be a positive number");
                return "redirect:/admin";
            }
            
            apiKey.setTokenLimitValue(tokenLimitValue);
            
            if (limitType == UserApiKey.TokenLimitType.TOTAL) {
                // Legacy behavior: use remainingTokens
                apiKey.setRemainingTokens(tokenLimitValue);
            } else if (limitType == UserApiKey.TokenLimitType.MONTHLY || limitType == UserApiKey.TokenLimitType.YEARLY) {
                // Reset period if changing limit type
                if (apiKey.getTokenLimitType() != limitType) {
                    apiKey.setTokensUsedThisPeriod(0);
                    apiKey.setPeriodStartDate(java.time.LocalDateTime.now());
                }
            } else if (limitType == UserApiKey.TokenLimitType.ONCE) {
                apiKey.setOneTimeUsed(false);
            }
        }
        
        apiKeyRepository.save(apiKey);
        
        redirectAttributes.addFlashAttribute("success", "API key updated successfully");
        return "redirect:/admin";
    }
    
    @PostMapping("/api-keys/{id}/delete")
    public String deleteApiKey(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        if (apiKeyRepository.existsById(id)) {
            apiKeyRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("success", "API key deleted");
        } else {
            redirectAttributes.addFlashAttribute("error", "API key not found");
        }
        
        return "redirect:/admin";
    }
}
