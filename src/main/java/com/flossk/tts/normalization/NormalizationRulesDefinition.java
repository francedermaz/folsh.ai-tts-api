package com.flossk.tts.normalization;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NormalizationRulesDefinition {

    private int version = 1;
    private boolean paragraphEndingPunctuation = true;
    private List<RegexRuleDefinition> preprocessing = new ArrayList<>();
    private List<RegexRuleDefinition> replacements = new ArrayList<>();
    private List<TokenRuleDefinition> tokens = new ArrayList<>();
    private List<String> doubleStandaloneLetters = new ArrayList<>();

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public boolean isParagraphEndingPunctuation() {
        return paragraphEndingPunctuation;
    }

    public void setParagraphEndingPunctuation(boolean paragraphEndingPunctuation) {
        this.paragraphEndingPunctuation = paragraphEndingPunctuation;
    }

    public List<RegexRuleDefinition> getPreprocessing() {
        return preprocessing;
    }

    public void setPreprocessing(List<RegexRuleDefinition> preprocessing) {
        this.preprocessing = preprocessing;
    }

    public List<RegexRuleDefinition> getReplacements() {
        return replacements;
    }

    public void setReplacements(List<RegexRuleDefinition> replacements) {
        this.replacements = replacements;
    }

    public List<TokenRuleDefinition> getTokens() {
        return tokens;
    }

    public void setTokens(List<TokenRuleDefinition> tokens) {
        this.tokens = tokens;
    }

    public List<String> getDoubleStandaloneLetters() {
        return doubleStandaloneLetters;
    }

    public void setDoubleStandaloneLetters(List<String> doubleStandaloneLetters) {
        this.doubleStandaloneLetters = doubleStandaloneLetters;
    }
}
