package com.flossk.tts.service;

import com.flossk.tts.normalization.NormalizationRulesService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TextNormalizationServiceTest {

    private TextNormalizationService service;

    @BeforeEach
    void setUp() throws Exception {
        NormalizationRulesService rulesService = new NormalizationRulesService(
            "./target/test-normalization-rules.json",
            tools.jackson.databind.json.JsonMapper.builder().build()
        );
        rulesService.reloadFromClasspathForTests();
        service = new TextNormalizationService(rulesService);
    }

    @Test
    void addsPeriodToParagraphsMissingEndingPunctuation() {
        String input = """
            Konsumi i rrymës në tetor u rrit për 17%, importi për 19% e prodhimi ra për 15%

            Konsumi i energjisë elektrike në Kosovë ka shënuar rritje prej 17% në tetor të këtij viti, krahasuar me tetorin e vitit të shkuar. Kjo është shoqëruar me rënie të prodhimit vendor dhe import të shtuar të energjisë. E gjithë kjo situatë ka rritur pasigurinë në sektorin energjetik dhe ka hapur debat për mundësinë e rritjes së tarifave të energjisë

            Për 17 për qind ka shënuar rritje konsumi i energjisë elektrike në tetor të këtij viti, krahasuar me muajin e njëjtë të vitit të kaluar.
            """;

        String result = service.ensureParagraphEndingPunctuation(input);

        assertEquals("""
            Konsumi i rrymës në tetor u rrit për 17%, importi për 19% e prodhimi ra për 15%.

            Konsumi i energjisë elektrike në Kosovë ka shënuar rritje prej 17% në tetor të këtij viti, krahasuar me tetorin e vitit të shkuar. Kjo është shoqëruar me rënie të prodhimit vendor dhe import të shtuar të energjisë. E gjithë kjo situatë ka rritur pasigurinë në sektorin energjetik dhe ka hapur debat për mundësinë e rritjes së tarifave të energjisë.

            Për 17 për qind ka shënuar rritje konsumi i energjisë elektrike në tetor të këtij viti, krahasuar me muajin e njëjtë të vitit të kaluar.
            """.strip(), result);
    }

    @Test
    void leavesParagraphsThatAlreadyEndWithPunctuation() {
        assertEquals("Ky është testi.", service.ensureParagraphEndingPunctuation("Ky është testi."));
        assertEquals("A vazhdoi?!", service.ensureParagraphEndingPunctuation("A vazhdoi?!"));
    }

    @Test
    void replacesPercentSign() {
        assertEquals("Rritja 50 për qind", service.normalizeForTts("Rritja 50%"));
        assertEquals("Rritja 50 për qind", service.normalizeForTts("Rritja 50 %"));
    }

    @Test
    void removesThousandSeparatorsFromNumbers() {
        assertEquals("616967 mega vat", service.normalizeForTts("616,967 MWh"));
        assertEquals("1234567 ton", service.normalizeForTts("1,234,567 ton"));
        assertEquals("616.33 mijë tonë", service.normalizeForTts("616.33 mijë tonë"));
        assertEquals("prej minus 14.77 për qind", service.normalizeForTts("prej -14.77%"));
    }

    @Test
    void replacesLeadingMinusOnNumbers() {
        assertEquals("rritje prej minus 14.77 për qind", service.normalizeForTts("rritje prej -14.77%"));
        assertEquals("niveli 5-10", service.normalizeForTts("niveli 5-10"));
    }

    @Test
    void replacesEnergyUnits() {
        assertEquals("Prodhimi 100 mega vat", service.normalizeForTts("Prodhimi 100 MWh"));
        assertEquals("Prodhimi 100 mega vat", service.normalizeForTts("Prodhimi 100 mwh"));
        assertEquals("Kapaciteti 5 kilo vat", service.normalizeForTts("Kapaciteti 5 kWh"));
        assertEquals("Centrali 500 mega vat", service.normalizeForTts("Centrali 500 MW"));
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
    void doublesStandaloneShortWords() {
        assertEquals("ai ee tha", service.normalizeForTts("ai e tha"));
        assertEquals("uu ndal", service.normalizeForTts("u ndal"));
        assertEquals("ii tha", service.normalizeForTts("i tha"));
        assertEquals("turizmi", service.normalizeForTts("turizmi"));
    }

    @Test
    void leavesRegularAlbanianTextUnchangedExceptStandaloneParticles() {
        String text = "Energjia e rinovueshme rritet çdo vit.";
        assertEquals("Energjia ee rinovueshme rritet çdo vit.", service.normalizeForTts(text));
    }
}
