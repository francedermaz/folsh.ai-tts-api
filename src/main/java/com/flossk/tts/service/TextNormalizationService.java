package com.flossk.tts.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
public class TextNormalizationService {

    private static final Logger logger = LoggerFactory.getLogger(TextNormalizationService.class);

    private static final String WORD_BOUNDARY_PREFIX = "(?<![A-Za-zÀ-ÖØ-öø-ÿ])";
    private static final String WORD_BOUNDARY_SUFFIX = "(?![A-Za-zÀ-ÖØ-öø-ÿ])";

    private record Rule(Pattern pattern, String replacement) {}

    private static final Rule[] RULES = {
        // Energy (longer units first)
        rule("GWh", "gigavat orë"),
        rule("MWh", "megavat"),
        rule("kWh", "kilovat"),
        rule("Wh", "vat orë"),
        rule("GW", "gigavat"),
        rule("MW", "megavat"),
        rule("kW", "kilovat"),

        // Speed and area
        rule("km/h", "kilometra në orë"),
        rule("km²", "kilometra katrorë"),
        rule("km2", "kilometra katrorë"),
        rule("m²", "metra katrorë"),
        rule("m2", "metra katrorë"),
        rule("cm²", "centimetra katrorë"),
        rule("cm2", "centimetra katrorë"),
        rule("ha", "hektarë"),

        // Length and mass
        rule("km", "kilometra"),
        rule("cm", "centimetra"),
        rule("mm", "milimetra"),
        rule("kg", "kilogram"),
        rule("mg", "miligram"),
        rule("lt", "litra"),
        rule("ml", "mililitra"),

        // Common abbreviations
        rule("p.sh.", "për shembull"),
        rule("p.sh", "për shembull"),
        rule("nr.", "numri"),
        rule("nr", "numri"),
        rule("etj.", "etjetera"),
        rule("etj", "etjetera"),
        rule("bashk.", "bashkë"),
        rule("bashk", "bashkë"),
        rule("prof.", "profesor"),
        rule("prof", "profesor"),
        rule("dr.", "doktor"),
        rule("dr", "doktor"),
    };

    private static final Pattern PERCENT = Pattern.compile("\\s*%");
    private static final Pattern AMPERSAND = Pattern.compile("\\s*&\\s*");
    private static final Pattern AT_SIGN = Pattern.compile("@");
    private static final Pattern EURO = Pattern.compile("€");
    private static final Pattern DOLLAR = Pattern.compile("\\$");
    private static final Pattern POUND = Pattern.compile("£");

    public String normalizeForTts(String text) {
        if (text == null) {
            return null;
        }

        String result = text;
        result = PERCENT.matcher(result).replaceAll(" për qind");
        result = AMPERSAND.matcher(result).replaceAll(" dhe ");
        result = AT_SIGN.matcher(result).replaceAll(" at ");
        result = EURO.matcher(result).replaceAll(" euro ");
        result = DOLLAR.matcher(result).replaceAll(" dollar ");
        result = POUND.matcher(result).replaceAll(" pound ");

        for (Rule rule : RULES) {
            result = rule.pattern().matcher(result).replaceAll(rule.replacement());
        }

        if (logger.isDebugEnabled() && !result.equals(text)) {
            logger.debug("Normalized text for TTS: '{}' -> '{}'", text, result);
        }

        return result;
    }

    private static Rule rule(String token, String spoken) {
        return new Rule(
            Pattern.compile(
                WORD_BOUNDARY_PREFIX + Pattern.quote(token) + WORD_BOUNDARY_SUFFIX,
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
            ),
            spoken
        );
    }
}
