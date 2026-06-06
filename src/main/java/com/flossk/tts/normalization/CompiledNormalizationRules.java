package com.flossk.tts.normalization;

import java.util.List;
import java.util.regex.Pattern;

public record CompiledNormalizationRules(
    boolean paragraphEndingPunctuation,
    Pattern paragraphBreak,
    List<CompiledRegexRule> preprocessing,
    List<CompiledRegexRule> replacements,
    List<CompiledTokenRule> tokens,
    List<Pattern> doubleStandaloneLetterPatterns
) {
}
