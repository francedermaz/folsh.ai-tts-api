package com.flossk.tts.service;

import com.flossk.tts.normalization.CompiledNormalizationRules;
import com.flossk.tts.normalization.CompiledRegexRule;
import com.flossk.tts.normalization.CompiledTokenRule;
import com.flossk.tts.normalization.NormalizationRulesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
public class TextNormalizationService {

    private static final Logger logger = LoggerFactory.getLogger(TextNormalizationService.class);

    private final NormalizationRulesService rulesService;

    public TextNormalizationService(NormalizationRulesService rulesService) {
        this.rulesService = rulesService;
    }

    public String ensureParagraphEndingPunctuation(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }

        CompiledNormalizationRules rules = rulesService.getRules();
        if (!rules.paragraphEndingPunctuation()) {
            return text.strip();
        }

        String[] paragraphs = rules.paragraphBreak().split(text.strip());
        StringBuilder result = new StringBuilder();

        for (String paragraph : paragraphs) {
            String trimmed = paragraph.strip();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (!endsWithTerminalPunctuation(trimmed)) {
                trimmed = trimmed + ".";
            }
            if (result.length() > 0) {
                result.append("\n\n");
            }
            result.append(trimmed);
        }

        return result.length() > 0 ? result.toString() : text.strip();
    }

    public String normalizeForTts(String text) {
        if (text == null) {
            return null;
        }

        CompiledNormalizationRules rules = rulesService.getRules();
        String result = text;

        result = applyRegexRules(result, rules.preprocessing());
        result = applyRegexRules(result, rules.replacements());

        for (CompiledTokenRule tokenRule : rules.tokens()) {
            result = tokenRule.pattern().matcher(result).replaceAll(tokenRule.replacement());
        }

        for (Pattern pattern : rules.doubleStandaloneLetterPatterns()) {
            result = doubleStandaloneLetter(result, pattern);
        }

        if (logger.isDebugEnabled() && !result.equals(text)) {
            logger.debug("Normalized text for TTS: '{}' -> '{}'", text, result);
        }

        return result;
    }

    private static String applyRegexRules(String text, java.util.List<CompiledRegexRule> regexRules) {
        String result = text;
        for (CompiledRegexRule rule : regexRules) {
            if (rule.repeatUntilStable()) {
                String previous;
                do {
                    previous = result;
                    result = rule.pattern().matcher(result).replaceAll(rule.replacement());
                } while (!result.equals(previous));
            } else {
                result = rule.pattern().matcher(result).replaceAll(rule.replacement());
            }
        }
        return result;
    }

    private static boolean endsWithTerminalPunctuation(String text) {
        if (text.isEmpty()) {
            return true;
        }
        char last = text.charAt(text.length() - 1);
        return last == '.' || last == '!' || last == '?' || last == '…';
    }

    private static String doubleStandaloneLetter(String text, Pattern pattern) {
        return pattern.matcher(text).replaceAll(match -> match.group() + match.group());
    }
}
