package com.flossk.tts.normalization;

import java.util.regex.Pattern;

public record CompiledTokenRule(
    Pattern pattern,
    String replacement,
    String note
) {
}
