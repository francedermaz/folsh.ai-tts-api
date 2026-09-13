package com.flossk.tts.service;

import io.github.givimad.piperjni.PiperJNI;
import io.github.givimad.piperjni.PiperJNI.NotInitialized;
import io.github.givimad.piperjni.PiperVoice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

@Service
public class TtsService {

    private static final Logger logger = LoggerFactory.getLogger(TtsService.class);
    private final VoiceManagerService voiceManagerService;
    private final TextNormalizationService textNormalizationService;
    private final MandatoryTextFixService mandatoryTextFixService;
    private final String cacheDirectory;
    private final boolean normalizationEnabled;
    private final String piperCommand;
    private final long piperTimeoutSeconds;

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

    public TtsService(
        VoiceManagerService voiceManagerService,
        TextNormalizationService textNormalizationService,
        MandatoryTextFixService mandatoryTextFixService,
        @Value("${tts.cache.directory:cache}") String cacheDirectory,
        @Value("${tts.normalization.enabled:true}") boolean normalizationEnabled,
        @Value("${tts.piper.command:}") String piperCommand,
        @Value("${tts.piper.timeout-seconds:60}") long piperTimeoutSeconds
    ) {
        this.voiceManagerService = voiceManagerService;
        this.textNormalizationService = textNormalizationService;
        this.mandatoryTextFixService = mandatoryTextFixService;
        this.cacheDirectory = cacheDirectory;
        this.normalizationEnabled = normalizationEnabled;
        this.piperCommand = piperCommand == null ? "" : piperCommand.trim();
        this.piperTimeoutSeconds = piperTimeoutSeconds;
        initializeCacheDirectory();

        if (!voiceManagerService.isUseJni() && this.piperCommand.isBlank()) {
            throw new IllegalStateException(
                "tts.piper.use-jni=false requires tts.piper.command to be set to the piper binary path"
            );
        }
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
     * Disabled when {@code tts.normalization.enabled=false}.
     */
    public String normalizeText(String text) {
        if (text == null) {
            return null;
        }
        if (!normalizationEnabled) {
            return text;
        }
        String withPunctuation = textNormalizationService.ensureParagraphEndingPunctuation(text);
        String withLineBreaks = withPunctuation.replace("\r\n", "'")
                                    .replace("\n", "'")
                                    .replace("\r", "'");
        return textNormalizationService.normalizeForTts(withLineBreaks);
    }

    public GenerationResult generateSpeech(String text, String voiceId) throws IOException {
        // Mandatory (always): fix "sentence.Next" spacing. Optional normalization is separate.
        String normalizedText = mandatoryTextFixService.ensureSpaceAfterSentencePunctuation(text);

        String cacheKey = generateCacheKey(normalizedText, voiceId);
        Path cacheFile = Paths.get(cacheDirectory, cacheKey + ".wav");

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

        if (!voiceManagerService.hasVoice(voiceId)) {
            throw new IllegalArgumentException("Voice not found: " + voiceId);
        }

        byte[] audioData;
        if (voiceManagerService.isUseJni()) {
            PiperVoice voice = voiceManagerService.getVoice(voiceId);
            if (voice == null) {
                throw new IllegalArgumentException("Voice not found: " + voiceId);
            }
            audioData = generateAudioWithPiperJni(normalizedText, voice);
            Files.write(cacheFile, audioData);
        } else {
            audioData = generateAudioWithPiperCli(normalizedText, voiceId, cacheFile);
        }

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

    private byte[] generateAudioWithPiperJni(String text, PiperVoice voice) {
        try {
            PiperJNI piperJNI = voiceManagerService.getPiperJNI();

            logger.debug("Generating audio via JNI for text: '{}' with voice: {}", text, voice);
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
            logger.error("Error generating audio with Piper JNI", e);
            throw new RuntimeException("Failed to generate audio: " + e.getMessage(), e);
        }
    }

    private byte[] generateAudioWithPiperCli(String text, String voiceId, Path cacheFile) throws IOException {
        Path modelPath = voiceManagerService.getModelPath(voiceId);
        if (modelPath == null || !Files.exists(modelPath)) {
            throw new IllegalArgumentException("Voice model not found: " + voiceId);
        }

        Path textFile = Files.createTempFile("piper-input-", ".txt");
        try {
            Files.writeString(textFile, text, StandardCharsets.UTF_8);
            logger.info("Piper CLI temp text file: {}", textFile.toAbsolutePath());
            Files.createDirectories(cacheFile.getParent());

            ProcessBuilder processBuilder = new ProcessBuilder(
                piperCommand,
                "-m", modelPath.toAbsolutePath().toString(),
                "-f", cacheFile.toAbsolutePath().toString()
            );
            processBuilder.redirectInput(textFile.toFile());
            processBuilder.redirectErrorStream(true);

            logger.debug(
                "Generating audio via CLI: {} -m {} -f {} < {}",
                piperCommand,
                modelPath,
                cacheFile,
                textFile
            );

            Process process = processBuilder.start();
            String processOutput;
            try (InputStream inputStream = process.getInputStream()) {
                processOutput = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            }

            boolean finished;
            try {
                finished = process.waitFor(piperTimeoutSeconds, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                process.destroyForcibly();
                throw new IOException("Interrupted while waiting for piper CLI", e);
            }

            if (!finished) {
                process.destroyForcibly();
                throw new IOException("Piper CLI timed out after " + piperTimeoutSeconds + "s");
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                throw new IOException(
                    "Piper CLI failed with exit code " + exitCode
                        + (processOutput.isBlank() ? "" : ": " + processOutput.trim())
                );
            }

            if (!Files.exists(cacheFile) || Files.size(cacheFile) == 0) {
                throw new IOException(
                    "Piper CLI did not write output file"
                        + (processOutput.isBlank() ? "" : ": " + processOutput.trim())
                );
            }

            logger.info("Generated audio via CLI for voice: {}, size: {} bytes", voiceId, Files.size(cacheFile));
            return Files.readAllBytes(cacheFile);
        } finally {
            Files.deleteIfExists(textFile);
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
            dos.writeBytes("RIFF");
            dos.writeInt(Integer.reverseBytes(fileSize));
            dos.writeBytes("WAVE");

            dos.writeBytes("fmt ");
            dos.writeInt(Integer.reverseBytes(16));
            dos.writeShort(Short.reverseBytes((short) 1));
            dos.writeShort(Short.reverseBytes((short) numChannels));
            dos.writeInt(Integer.reverseBytes(sampleRate));
            dos.writeInt(Integer.reverseBytes(sampleRate * numChannels * bitsPerSample / 8));
            dos.writeShort(Short.reverseBytes((short) (numChannels * bitsPerSample / 8)));
            dos.writeShort(Short.reverseBytes((short) bitsPerSample));

            dos.writeBytes("data");
            dos.writeInt(Integer.reverseBytes(dataSize));

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
