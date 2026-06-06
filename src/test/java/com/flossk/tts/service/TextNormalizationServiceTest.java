package com.flossk.tts.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TextNormalizationServiceTest {

    private TextNormalizationService service;

    @BeforeEach
    void setUp() {
        service = new TextNormalizationService();
    }

    @Test
    void replacesPercentSign() {
        assertEquals("Rritja 50 për qind", service.normalizeForTts("Rritja 50%"));
        assertEquals("Rritja 50 për qind", service.normalizeForTts("Rritja 50 %"));
    }

    @Test
    void replacesEnergyUnits() {
        assertEquals("Prodhimi 100 megavat", service.normalizeForTts("Prodhimi 100 MWh"));
        assertEquals("Prodhimi 100 megavat", service.normalizeForTts("Prodhimi 100 mwh"));
        assertEquals("Kapaciteti 5 kilovat", service.normalizeForTts("Kapaciteti 5 kWh"));
        assertEquals("Centrali 500 megavat", service.normalizeForTts("Centrali 500 MW"));
    }

    @Test
    void replacesCurrencyAndSymbols() {
        assertEquals("Çmimi 10 euro ", service.normalizeForTts("Çmimi 10€"));
        assertEquals("A dhe B", service.normalizeForTts("A & B"));
    }

    @Test
    void replacesCommonAbbreviations() {
        assertEquals("për shembull energjia", service.normalizeForTts("p.sh. energjia"));
        assertEquals("numri 5", service.normalizeForTts("nr. 5"));
    }

    @Test
    void leavesRegularAlbanianTextUnchanged() {
        String text = "Energjia e rinovueshme rritet çdo vit.";
        assertEquals(text, service.normalizeForTts(text));
    }
}
