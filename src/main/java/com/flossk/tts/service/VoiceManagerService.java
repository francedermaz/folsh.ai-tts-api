package com.flossk.tts.service;

import io.github.givimad.piperjni.PiperJNI;
import io.github.givimad.piperjni.PiperJNI.NotInitialized;
import io.github.givimad.piperjni.PiperVoice;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@Service
public class VoiceManagerService {
    
    private static final Logger logger = LoggerFactory.getLogger(VoiceManagerService.class);
    private final Map<String, PiperVoice> voiceSessions = new ConcurrentHashMap<>();
    private final Map<String, Path> tempOnnxFiles = new ConcurrentHashMap<>();
    private final Map<String, Path> tempJsonFiles = new ConcurrentHashMap<>();
    private final ResourceLoader resourceLoader;
    private PiperJNI piperJNI;
    
    public VoiceManagerService(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
        initializeVoiceSessions();
    }
    
    private void initializeVoiceSessions() {
        try {
            piperJNI = new PiperJNI();
            piperJNI.initialize();
            
            String[] requiredVoiceIds = {"edon", "arta", "arben", "dren"};
            
            for (String voiceId : requiredVoiceIds) {
                try {
                    Resource onnxResource = resourceLoader.getResource("classpath:voices/" + voiceId + ".onnx");
                    Resource jsonResource = resourceLoader.getResource("classpath:voices/" + voiceId + ".onnx.json");
                    
                    if (!onnxResource.exists()) {
                        String errorMsg = String.format("Required voice model not found: %s.onnx in classpath:voices/", voiceId);
                        logger.error(errorMsg);
                        throw new IllegalStateException(errorMsg);
                    }
                    
                    if (!jsonResource.exists()) {
                        String errorMsg = String.format("Required voice config not found: %s.onnx.json in classpath:voices/", voiceId);
                        logger.error(errorMsg);
                        throw new IllegalStateException(errorMsg);
                    }
                    
                    Path tempOnnxFile = Files.createTempFile("voice_" + voiceId, ".onnx");
                    Path tempJsonFile = Files.createTempFile("voice_" + voiceId, ".onnx.json");
                    
                    Files.copy(onnxResource.getInputStream(), tempOnnxFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    Files.copy(jsonResource.getInputStream(), tempJsonFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    
                    tempOnnxFiles.put(voiceId, tempOnnxFile);
                    tempJsonFiles.put(voiceId, tempJsonFile);
                    
                    PiperVoice voice = piperJNI.loadVoice(tempOnnxFile, tempJsonFile, 0);
                    voiceSessions.put(voiceId, voice);
                    
                    logger.info("Successfully loaded voice model: {}", voiceId);
                } catch (IOException | NotInitialized e) {
                    String errorMsg = String.format("Failed to initialize Piper voice: %s", voiceId);
                    logger.error(errorMsg, e);
                    throw new IllegalStateException(errorMsg, e);
                }
            }
            
            if (voiceSessions.size() != requiredVoiceIds.length) {
                String errorMsg = String.format("Failed to load all required voices. Expected %d, loaded %d", 
                    requiredVoiceIds.length, voiceSessions.size());
                logger.error(errorMsg);
                throw new IllegalStateException(errorMsg);
            }
            
            logger.info("Successfully initialized all {} voice sessions: {}", voiceSessions.size(), voiceSessions.keySet());
            
        } catch (IllegalStateException e) {
            logger.error("CRITICAL: Application cannot start without all required voice models", e);
            throw e;
        } catch (Exception e) {
            logger.error("Failed to initialize voice sessions", e);
            throw new IllegalStateException("Failed to initialize voice sessions", e);
        }
    }
    
    public PiperVoice getVoice(String voiceId) {
        return voiceSessions.get(voiceId);
    }
    
    public boolean hasVoice(String voiceId) {
        return voiceSessions.containsKey(voiceId);
    }
    
    public PiperJNI getPiperJNI() {
        return piperJNI;
    }
    
    @PreDestroy
    public void cleanup() {
        logger.info("Cleaning up voice sessions...");
        
        for (Map.Entry<String, PiperVoice> entry : voiceSessions.entrySet()) {
            try {
                entry.getValue().close();
                logger.info("Closed voice: {}", entry.getKey());
            } catch (Exception e) {
                logger.error("Error closing voice: {}", entry.getKey(), e);
            }
        }
        
        if (piperJNI != null) {
            try {
                piperJNI.terminate();
                piperJNI.close();
            } catch (Exception e) {
                logger.error("Error terminating PiperJNI", e);
            }
        }
        
        for (Map.Entry<String, Path> entry : tempOnnxFiles.entrySet()) {
            try {
                Files.deleteIfExists(entry.getValue());
            } catch (IOException e) {
                logger.warn("Failed to delete temp file: {}", entry.getValue(), e);
            }
        }
        
        for (Map.Entry<String, Path> entry : tempJsonFiles.entrySet()) {
            try {
                Files.deleteIfExists(entry.getValue());
            } catch (IOException e) {
                logger.warn("Failed to delete temp file: {}", entry.getValue(), e);
            }
        }
        
        voiceSessions.clear();
        tempOnnxFiles.clear();
        tempJsonFiles.clear();
    }
}
