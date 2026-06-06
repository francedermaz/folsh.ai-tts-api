package com.flossk.tts.service;

import io.github.givimad.piperjni.PiperJNI;
import io.github.givimad.piperjni.PiperJNI.NotInitialized;
import io.github.givimad.piperjni.PiperVoice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class TtsService {
    
    private static final Logger logger = LoggerFactory.getLogger(TtsService.class);
    private final VoiceManagerService voiceManagerService;
    private final TextNormalizationService textNormalizationService;
    private final String cacheDirectory;
    
    public static class GenerationResult {
        private final byte[] audioData;
        private final boolean cached;
        private final String textHash;
        
        public GenerationResult(byte[] audioData, boolean cached, String textHash) {
            this.audioData = audioData;
            this.cached = cached;
            this.textHash = textHash;
        }
        
        public byte[] getAudioData() {
            return audioData;
        }
        
        public boolean isCached() {
            return cached;
        }
        
        public String getTextHash() {
            return textHash;
        }
    }
    
    public TtsService(VoiceManagerService voiceManagerService,
                     TextNormalizationService textNormalizationService,
                     @Value("${tts.cache.directory:cache}") String cacheDirectory) {
        this.voiceManagerService = voiceManagerService;
        this.textNormalizationService = textNormalizationService;
        this.cacheDirectory = cacheDirectory;
        initializeCacheDirectory();
    }
    
    private void initializeCacheDirectory() {
        try {
            Path cachePath = Paths.get(cacheDirectory);
            if (!Files.exists(cachePath)) {
                Files.createDirectories(cachePath);
                logger.info("Created cache directory: {}", cacheDirectory);
            }
        } catch (IOException e) {
            logger.error("Failed to create cache directory", e);
        }
    }
    
    /**
     * Normalizes text for TTS processing: line breaks and Albanian pronunciation rules.
     */
    public String normalizeText(String text) {
        if (text == null) {
            return null;
        }
        String withLineBreaks = text.replace("\r\n", "'")
                                    .replace("\n", "'")
                                    .replace("\r", "'");
        return textNormalizationService.normalizeForTts(withLineBreaks);
    }
    
    public GenerationResult generateSpeech(String text, String voiceId) throws IOException {
        // Normalize text before TTS
        String normalizedText = normalizeText(text);
        
        String cacheKey = generateCacheKey(normalizedText, voiceId);
        Path cacheFile = Paths.get(cacheDirectory, cacheKey + ".wav");
        boolean cached = false;
        
        if (Files.exists(cacheFile)) {
            Instant fileTime = Files.getLastModifiedTime(cacheFile).toInstant();
            Instant threeDaysAgo = Instant.now().minus(3, ChronoUnit.DAYS);
            
            if (fileTime.isAfter(threeDaysAgo)) {
                logger.debug("Serving cached audio for voice: {}, text hash: {}", voiceId, cacheKey);
                byte[] audioData = Files.readAllBytes(cacheFile);
                return new GenerationResult(audioData, true, extractHashFromCacheKey(cacheKey));
            } else {
                logger.debug("Cache expired, regenerating audio");
                Files.delete(cacheFile);
            }
        }
        
        PiperVoice voice = voiceManagerService.getVoice(voiceId);
        if (voice == null) {
            throw new IllegalArgumentException("Voice not found: " + voiceId);
        }
        
        byte[] audioData = generateAudioWithPiper(normalizedText, voice);
        
        Files.write(cacheFile, audioData);
        logger.info("Generated and cached audio for voice: {}, text hash: {}", voiceId, cacheKey);
        
        return new GenerationResult(audioData, false, extractHashFromCacheKey(cacheKey));
    }
    
    private String extractHashFromCacheKey(String cacheKey) {
        int underscoreIndex = cacheKey.indexOf('_');
        if (underscoreIndex >= 0 && underscoreIndex < cacheKey.length() - 1) {
            return cacheKey.substring(underscoreIndex + 1);
        }
        return cacheKey;
    }
    
    private byte[] generateAudioWithPiper(String text, PiperVoice voice) {
        try {
            PiperJNI piperJNI = voiceManagerService.getPiperJNI();
            
            logger.debug("Generating audio for text: '{}' with voice: {}", text, voice);
            short[] samples = piperJNI.textToAudio(voice, text);
            
            if (samples == null || samples.length == 0) {
                logger.error("Piper returned empty or null audio samples for text: '{}'", text);
                throw new RuntimeException("Generated audio is empty");
            }
            
            int sampleRate = voice.getSampleRate();
            logger.info("Generated {} audio samples at {} Hz sample rate", samples.length, sampleRate);
            
            byte[] wavData = convertToWav(samples, sampleRate);
            logger.debug("Converted to WAV format, size: {} bytes", wavData.length);
            
            return wavData;
            
        } catch (IOException | NotInitialized e) {
            logger.error("Error generating audio with Piper", e);
            throw new RuntimeException("Failed to generate audio: " + e.getMessage(), e);
        }
    }
    
    private byte[] convertToWav(short[] samples, int sampleRate) {
        if (samples == null || samples.length == 0) {
            logger.error("Cannot convert empty samples to WAV");
            throw new IllegalArgumentException("Samples array is null or empty");
        }
        
        int numChannels = 1;
        int bitsPerSample = 16;
        int dataSize = samples.length * 2;
        int fileSize = 36 + dataSize;
        
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        java.io.DataOutputStream dos = new java.io.DataOutputStream(baos);
        
        try {
            // RIFF header
            dos.writeBytes("RIFF");
            dos.writeInt(Integer.reverseBytes(fileSize));
            dos.writeBytes("WAVE");
            
            // fmt chunk
            dos.writeBytes("fmt ");
            dos.writeInt(Integer.reverseBytes(16)); // fmt chunk size
            dos.writeShort(Short.reverseBytes((short) 1)); // audio format (1 = PCM)
            dos.writeShort(Short.reverseBytes((short) numChannels)); // number of channels
            dos.writeInt(Integer.reverseBytes(sampleRate)); // sample rate
            dos.writeInt(Integer.reverseBytes(sampleRate * numChannels * bitsPerSample / 8)); // byte rate
            dos.writeShort(Short.reverseBytes((short) (numChannels * bitsPerSample / 8))); // block align
            dos.writeShort(Short.reverseBytes((short) bitsPerSample)); // bits per sample
            
            // data chunk
            dos.writeBytes("data");
            dos.writeInt(Integer.reverseBytes(dataSize));
            
            // Write audio samples
            for (short sample : samples) {
                dos.writeShort(Short.reverseBytes(sample));
            }
            
            dos.flush();
            dos.close();
            
            byte[] wavBytes = baos.toByteArray();
            logger.debug("WAV file created: {} bytes, {} samples", wavBytes.length, samples.length);
            
            return wavBytes;
            
        } catch (IOException e) {
            logger.error("Error converting to WAV format", e);
            throw new RuntimeException("Failed to convert audio to WAV", e);
        }
    }
    
    private String generateCacheKey(String text, String voiceId) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String input = voiceId + "_" + text;
            byte[] hash = digest.digest(input.getBytes());
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return voiceId + "_" + hexString.toString();
            
        } catch (NoSuchAlgorithmException e) {
            logger.error("SHA-256 algorithm not found", e);
            return voiceId + "_" + text.hashCode();
        }
    }
    
    public Path getCacheDirectory() {
        return Paths.get(cacheDirectory);
    }
}
