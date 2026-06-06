package com.flossk.tts.normalization;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class NormalizationRulesService {

    private static final Logger logger = LoggerFactory.getLogger(NormalizationRulesService.class);
    private static final String DEFAULT_CLASSPATH_RULES = "tts/normalization-rules.json";

    private final Path rulesPath;
    private final JsonMapper jsonMapper;
    private final AtomicReference<CompiledNormalizationRules> compiledRules = new AtomicReference<>();
    private volatile long lastModified = -1L;

    public NormalizationRulesService(
        @Value("${tts.normalization.rules-path:./config/normalization-rules.json}") String rulesPath,
        JsonMapper jsonMapper
    ) {
        this.rulesPath = Path.of(rulesPath).toAbsolutePath().normalize();
        this.jsonMapper = jsonMapper;
    }

    @PostConstruct
    public void initialize() throws IOException {
        ensureRulesFileExists();
        reload();
    }

    public CompiledNormalizationRules getRules() {
        CompiledNormalizationRules rules = compiledRules.get();
        if (rules == null) {
            throw new IllegalStateException("Normalization rules are not loaded");
        }
        return rules;
    }

    public Path getRulesPath() {
        return rulesPath;
    }

    public String readRulesFileContent() throws IOException {
        ensureRulesFileExists();
        return Files.readString(rulesPath);
    }

    public NormalizationRulesDefinition loadRulesDefinition() throws IOException {
        ensureRulesFileExists();
        try (InputStream inputStream = Files.newInputStream(rulesPath)) {
            return jsonMapper.readValue(inputStream, NormalizationRulesDefinition.class);
        }
    }

    public void saveRulesFileContent(String content) throws IOException {
        saveRulesDefinition(jsonMapper.readValue(content, NormalizationRulesDefinition.class));
    }

    public void saveRulesDefinition(NormalizationRulesDefinition definition) throws IOException {
        NormalizationRulesDefinition sanitized = sanitize(definition);
        NormalizationRulesCompiler.compile(sanitized);
        Files.writeString(rulesPath, jsonMapper.writeValueAsString(sanitized));
        reload();
    }

    public void resetToDefaults() throws IOException {
        ClassPathResource resource = new ClassPathResource(DEFAULT_CLASSPATH_RULES);
        try (InputStream inputStream = resource.getInputStream()) {
            Files.copy(inputStream, rulesPath, StandardCopyOption.REPLACE_EXISTING);
        }
        reload();
        logger.info("Reset normalization rules to bundled defaults at {}", rulesPath);
    }

    @Scheduled(fixedDelayString = "${tts.normalization.rules-check-interval-ms:2000}")
    public void checkForUpdates() {
        if (!Files.exists(rulesPath)) {
            return;
        }
        try {
            long modified = Files.getLastModifiedTime(rulesPath).toMillis();
            if (modified > lastModified) {
                reload();
            }
        } catch (IOException e) {
            logger.warn("Failed to check normalization rules file timestamp: {}", e.getMessage());
        }
    }

    public synchronized void reload() throws IOException {
        if (!Files.exists(rulesPath)) {
            ensureRulesFileExists();
        }

        NormalizationRulesDefinition definition;
        try (InputStream inputStream = Files.newInputStream(rulesPath)) {
            definition = jsonMapper.readValue(inputStream, NormalizationRulesDefinition.class);
        }

        CompiledNormalizationRules compiled = NormalizationRulesCompiler.compile(definition);
        compiledRules.set(compiled);
        lastModified = Files.getLastModifiedTime(rulesPath).toMillis();

        logger.info(
            "Loaded normalization rules v{} from {} ({} preprocessing, {} replacements, {} tokens)",
            definition.getVersion(),
            rulesPath,
            compiled.preprocessing().size(),
            compiled.replacements().size(),
            compiled.tokens().size()
        );
    }

    public void reloadFromClasspathForTests() throws IOException {
        try (InputStream inputStream = new ClassPathResource(DEFAULT_CLASSPATH_RULES).getInputStream()) {
            NormalizationRulesDefinition definition = jsonMapper.readValue(inputStream, NormalizationRulesDefinition.class);
            compiledRules.set(NormalizationRulesCompiler.compile(definition));
            lastModified = System.currentTimeMillis();
        }
    }

    private NormalizationRulesDefinition sanitize(NormalizationRulesDefinition definition) {
        NormalizationRulesDefinition sanitized = new NormalizationRulesDefinition();
        sanitized.setVersion(definition.getVersion());
        sanitized.setParagraphEndingPunctuation(definition.isParagraphEndingPunctuation());
        sanitized.setPreprocessing(filterRegexRules(definition.getPreprocessing()));
        sanitized.setReplacements(filterRegexRules(definition.getReplacements()));
        sanitized.setTokens(filterTokenRules(definition.getTokens()));
        sanitized.setDoubleStandaloneLetters(filterLetters(definition.getDoubleStandaloneLetters()));
        return sanitized;
    }

    private List<RegexRuleDefinition> filterRegexRules(List<RegexRuleDefinition> rules) {
        if (rules == null) {
            return new ArrayList<>();
        }
        return rules.stream()
            .filter(rule -> rule.getPattern() != null && !rule.getPattern().isBlank())
            .toList();
    }

    private List<TokenRuleDefinition> filterTokenRules(List<TokenRuleDefinition> rules) {
        if (rules == null) {
            return new ArrayList<>();
        }
        return rules.stream()
            .filter(rule -> rule.getMatch() != null && !rule.getMatch().isBlank())
            .toList();
    }

    private List<String> filterLetters(List<String> letters) {
        if (letters == null) {
            return new ArrayList<>();
        }
        return letters.stream()
            .filter(letter -> letter != null && !letter.isBlank())
            .map(String::trim)
            .toList();
    }

    private void ensureRulesFileExists() throws IOException {
        if (Files.exists(rulesPath)) {
            return;
        }

        Path parent = rulesPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        ClassPathResource resource = new ClassPathResource(DEFAULT_CLASSPATH_RULES);
        if (!resource.exists()) {
            throw new IllegalStateException("Default normalization rules not found on classpath: " + DEFAULT_CLASSPATH_RULES);
        }

        try (InputStream inputStream = resource.getInputStream()) {
            Files.copy(inputStream, rulesPath, StandardCopyOption.REPLACE_EXISTING);
        }

        logger.info("Created default normalization rules file at {}", rulesPath);
    }
}
