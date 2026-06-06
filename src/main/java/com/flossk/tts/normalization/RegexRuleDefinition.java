package com.flossk.tts.normalization;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RegexRuleDefinition {

    private String pattern;
    private String replacement;
    private boolean repeatUntilStable;
    private String note;

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public String getReplacement() {
        return replacement;
    }

    public void setReplacement(String replacement) {
        this.replacement = replacement;
    }

    public boolean isRepeatUntilStable() {
        return repeatUntilStable;
    }

    public void setRepeatUntilStable(boolean repeatUntilStable) {
        this.repeatUntilStable = repeatUntilStable;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
