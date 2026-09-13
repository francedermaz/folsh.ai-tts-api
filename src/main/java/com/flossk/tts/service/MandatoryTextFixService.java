package com.flossk.tts.service;

import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

/**
 * Mandatory text fixes applied before Piper, independent of optional normalization rules.
 */
@Service
public class MandatoryTextFixService {

    /** "word.Next" / "word.next" → "word. Next"; skips single-letter abbrevs like p.sh. */
    private static final Pattern MISSING_SPACE_AFTER_SENTENCE_PUNCTUATION =
        Pattern.compile("(?<=\\p{L}{2,})([.!?])(\\p{L})");

    /**
     * Fixes missing space after sentence-ending punctuation (e.g. {@code word.Next} → {@code word. Next}).
     * Does not change decimals like {@code 616.33}.
     */
    public String ensureSpaceAfterSentencePunctuation(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return MISSING_SPACE_AFTER_SENTENCE_PUNCTUATION.matcher(text).replaceAll("$1 $2");
    }
}
