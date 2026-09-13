package com.flossk.tts.service;

import io.github.givimad.piperjni.PiperJNI;
import io.github.givimad.piperjni.PiperJNI.NotInitialized;
import io.github.givimad.piperjni.PiperVoice;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class VoiceManagerService {

    private static final Logger logger = LoggerFactory.getLogger(VoiceManagerService.class);
    private static final Set<String> REQUIRED_VOICE_IDS = Set.of("edon", "arta", "arben", "dren");

    private final Map<String, PiperVoice> voiceSessions = new ConcurrentHashMap<>();
    private final Map<String, Path> modelPaths = new ConcurrentHashMap<>();
    private final Map<String, Path> configPaths = new ConcurrentHashMap<>();
    private final ResourceLoader resourceLoader;
    private final boolean useJni;
    private final Path modelsDirectory;
    private PiperJNI piperJNI;

    public VoiceManagerService(
        ResourceLoader resourceLoader,
        @Value("${tts.piper.use-jni:true}") boolean useJni,
        @Value("${tts.piper.models-directory:./models}") String modelsDirectory
    ) {
        this.resourceLoader = resourceLoader;
        this.useJni = useJni;
        this.modelsDirectory = Path.of(modelsDirectory).toAbsolutePath().normalize();
        initializeVoiceSessions();
    }

    private void initializeVoiceSessions() {
        if (!useJni) {
            logger.info(
                "Piper backend: CLI (JNI disabled). Models are not loaded at runtime; expected under {}",
                modelsDirectory
            );
            return;
        }

        try {
            Files.createDirectories(modelsDirectory);
            piperJNI = new PiperJNI();
            piperJNI.initialize();
            logger.info("Piper backend: JNI");

            for (String voiceId : REQUIRED_VOICE_IDS) {
                extractVoiceFiles(voiceId);
                try {
                    PiperVoice voice = piperJNI.loadVoice(
                        modelPaths.get(voiceId),
                        configPaths.get(voiceId),
                        0
                    );
                    voiceSessions.put(voiceId, voice);
                    logger.info("Successfully loaded voice model via JNI: {}", voiceId);
                } catch (IOException | NotInitialized e) {
                    throw new IllegalStateException("Failed to initialize Piper voice: " + voiceId, e);
                }
            }

            if (voiceSessions.size() != REQUIRED_VOICE_IDS.size()) {
                throw new IllegalStateException(String.format(
                    "Failed to load all required voices. Expected %d, loaded %d",
                    REQUIRED_VOICE_IDS.size(),
                    voiceSessions.size()
                ));
            }

            logger.info(
                "Successfully prepared {} voices: {}",
                REQUIRED_VOICE_IDS.size(),
                String.join(", ", REQUIRED_VOICE_IDS)
            );
        } catch (IllegalStateException e) {
            logger.error("CRITICAL: Application cannot start without all required voice models", e);
            throw e;
        } catch (Exception e) {
            logger.error("Failed to initialize voice sessions", e);
            throw new IllegalStateException("Failed to initialize voice sessions", e);
        }
    }

    private void extractVoiceFiles(String voiceId) throws IOException {
        Resource onnxResource = resourceLoader.getResource("classpath:voices/" + voiceId + ".onnx");
        Resource jsonResource = resourceLoader.getResource("classpath:voices/" + voiceId + ".onnx.json");

        if (!onnxResource.exists()) {
            throw new IllegalStateException(
                "Required voice model not found: " + voiceId + ".onnx in classpath:voices/"
            );
        }
        if (!jsonResource.exists()) {
            throw new IllegalStateException(
                "Required voice config not found: " + voiceId + ".onnx.json in classpath:voices/"
            );
        }

        Path onnxPath = modelsDirectory.resolve(voiceId + ".onnx");
        Path jsonPath = modelsDirectory.resolve(voiceId + ".onnx.json");

        Files.copy(onnxResource.getInputStream(), onnxPath, StandardCopyOption.REPLACE_EXISTING);
        Files.copy(jsonResource.getInputStream(), jsonPath, StandardCopyOption.REPLACE_EXISTING);

        modelPaths.put(voiceId, onnxPath);
        configPaths.put(voiceId, jsonPath);
    }

    public boolean isUseJni() {
        return useJni;
    }

    public Path getModelPath(String voiceId) {
        if (useJni) {
            return modelPaths.get(voiceId);
        }
        if (!REQUIRED_VOICE_IDS.contains(voiceId)) {
            return null;
        }
        Path onnxPath = modelsDirectory.resolve(voiceId + ".onnx");
        return Files.exists(onnxPath) ? onnxPath : null;
    }

    public PiperVoice getVoice(String voiceId) {
        return voiceSessions.get(voiceId);
    }

    public boolean hasVoice(String voiceId) {
        if (!REQUIRED_VOICE_IDS.contains(voiceId)) {
            return false;
        }
        if (useJni) {
            return voiceSessions.containsKey(voiceId);
        }
        Path onnxPath = modelsDirectory.resolve(voiceId + ".onnx");
        Path jsonPath = modelsDirectory.resolve(voiceId + ".onnx.json");
        return Files.exists(onnxPath) && Files.exists(jsonPath);
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

        voiceSessions.clear();
        modelPaths.clear();
        configPaths.clear();
    }
}
