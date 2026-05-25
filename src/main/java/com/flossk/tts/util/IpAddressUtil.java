package com.flossk.tts.util;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for extracting client IP addresses from HTTP requests.
 * Handles Cloudflare and other proxy headers correctly.
 */
public class IpAddressUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(IpAddressUtil.class);
    
    /**
     * Extracts the client IP address from the request, checking headers in order:
     * 1. CF-Connecting-IP (Cloudflare)
     * 2. True-Client-IP (Cloudflare Enterprise)
     * 3. X-Forwarded-For (standard proxy header)
     * 4. X-Real-IP (nginx proxy)
     * 5. Remote address (direct connection)
     * 
     * @param request The HTTP servlet request
     * @return The client IP address, or null if unable to determine
     */
    public static String getClientIpAddress(HttpServletRequest request) {
        // Cloudflare header (most reliable)
        String cfConnectingIp = request.getHeader("CF-Connecting-IP");
        if (cfConnectingIp != null && !cfConnectingIp.isBlank()) {
            String ip = cfConnectingIp.trim();
            logger.debug("Using CF-Connecting-IP: {}", ip);
            return ip;
        }
        
        // Cloudflare Enterprise header
        String trueClientIp = request.getHeader("True-Client-IP");
        if (trueClientIp != null && !trueClientIp.isBlank()) {
            String ip = trueClientIp.trim();
            logger.debug("Using True-Client-IP: {}", ip);
            return ip;
        }
        
        // X-Forwarded-For header (may contain multiple IPs, take the first one)
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            // X-Forwarded-For can contain multiple IPs separated by commas
            // The first IP is usually the original client IP
            String ip = xForwardedFor.split(",")[0].trim();
            if (!ip.isBlank()) {
                logger.debug("Using X-Forwarded-For (first IP): {}", ip);
                return ip;
            }
        }
        
        // X-Real-IP header (nginx proxy)
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            String ip = xRealIp.trim();
            logger.debug("Using X-Real-IP: {}", ip);
            return ip;
        }
        
        // Fallback to remote address (direct connection or last proxy)
        String remoteAddr = request.getRemoteAddr();
        logger.debug("Using remote address: {}", remoteAddr);
        return remoteAddr;
    }
}
