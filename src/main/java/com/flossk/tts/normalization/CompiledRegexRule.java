package com.flossk.tts.normalization;

import java.util.regex.Pattern;

public record CompiledRegexRule(
    Pattern pattern,
    String replacement,
    boolean repeatUntilStable,
    String note
) {
}
