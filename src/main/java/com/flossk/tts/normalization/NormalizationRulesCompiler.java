package com.flossk.tts.normalization;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

final class NormalizationRulesCompiler {

    private static final String WORD_BOUNDARY_PREFIX = "(?<![A-Za-zÀ-ÖØ-öø-ÿ])";
    private static final String WORD_BOUNDARY_SUFFIX = "(?![A-Za-zÀ-ÖØ-öø-ÿ])";
    private static final int REGEX_FLAGS = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;

    private NormalizationRulesCompiler() {
    }

    static CompiledNormalizationRules compile(NormalizationRulesDefinition definition) {
        List<CompiledRegexRule> preprocessing = compileRegexRules(definition.getPreprocessing(), "preprocessing");
        List<CompiledRegexRule> replacements = compileRegexRules(definition.getReplacements(), "replacements");
        List<CompiledTokenRule> tokens = compileTokenRules(definition.getTokens());
        List<Pattern> doubleLetters = compileDoubleStandaloneLetters(definition.getDoubleStandaloneLetters());

        return new CompiledNormalizationRules(
            definition.isParagraphEndingPunctuation(),
            Pattern.compile("(?:\\R\\s*){2,}"),
            preprocessing,
            replacements,
            tokens,
            doubleLetters
        );
    }

    private static List<CompiledRegexRule> compileRegexRules(List<RegexRuleDefinition> rules, String section) {
        List<CompiledRegexRule> compiled = new ArrayList<>();
        if (rules == null) {
            return compiled;
        }
        for (int i = 0; i < rules.size(); i++) {
            RegexRuleDefinition rule = rules.get(i);
            if (rule.getPattern() == null || rule.getPattern().isBlank()) {
                throw new IllegalArgumentException(section + " rule #" + (i + 1) + " is missing pattern");
            }
            if (rule.getReplacement() == null) {
                throw new IllegalArgumentException(section + " rule #" + (i + 1) + " is missing replacement");
            }
            try {
                compiled.add(new CompiledRegexRule(
                    Pattern.compile(rule.getPattern()),
                    rule.getReplacement(),
                    rule.isRepeatUntilStable(),
                    rule.getNote()
                ));
            } catch (PatternSyntaxException e) {
                throw new IllegalArgumentException(
                    section + " rule #" + (i + 1) + " has invalid pattern: " + rule.getPattern(),
                    e
                );
            }
        }
        return compiled;
    }

    private static List<CompiledTokenRule> compileTokenRules(List<TokenRuleDefinition> rules) {
        List<CompiledTokenRule> compiled = new ArrayList<>();
        if (rules == null) {
            return compiled;
        }
        for (int i = 0; i < rules.size(); i++) {
            TokenRuleDefinition rule = rules.get(i);
            if (rule.getMatch() == null || rule.getMatch().isBlank()) {
                throw new IllegalArgumentException("tokens rule #" + (i + 1) + " is missing match");
            }
            if (rule.getSay() == null) {
                throw new IllegalArgumentException("tokens rule #" + (i + 1) + " is missing say");
            }
            String pattern = WORD_BOUNDARY_PREFIX + Pattern.quote(rule.getMatch()) + WORD_BOUNDARY_SUFFIX;
            compiled.add(new CompiledTokenRule(
                Pattern.compile(pattern, REGEX_FLAGS),
                rule.getSay(),
                rule.getNote()
            ));
        }
        return compiled;
    }

    private static List<Pattern> compileDoubleStandaloneLetters(List<String> letters) {
        List<Pattern> compiled = new ArrayList<>();
        if (letters == null) {
            return compiled;
        }
        for (String letter : letters) {
            if (letter == null || letter.length() != 1) {
                continue;
            }
            compiled.add(Pattern.compile(
                WORD_BOUNDARY_PREFIX + letter + WORD_BOUNDARY_SUFFIX,
                REGEX_FLAGS
            ));
        }
        return compiled;
    }
}
