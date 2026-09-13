package com.flossk.tts.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MandatoryTextFixServiceTest {

    private MandatoryTextFixService service;

    @BeforeEach
    void setUp() {
        service = new MandatoryTextFixService();
    }

    @Test
    void insertsSpaceAfterPeriodStuckToNextSentence() {
        assertEquals(
            "ksjd kjsd kjsd. kssjkd kjsdk dkjsd",
            service.ensureSpaceAfterSentencePunctuation("ksjd kjsd kjsd.kssjkd kjsdk dkjsd")
        );
        assertEquals("Hello! World", service.ensureSpaceAfterSentencePunctuation("Hello!World"));
        assertEquals("Ask? Why", service.ensureSpaceAfterSentencePunctuation("Ask?Why"));
        assertEquals("616.33 mijë", service.ensureSpaceAfterSentencePunctuation("616.33 mijë"));
        assertEquals("already spaced. Fine", service.ensureSpaceAfterSentencePunctuation("already spaced. Fine"));
        assertEquals("p.sh. energjia", service.ensureSpaceAfterSentencePunctuation("p.sh. energjia"));
    }
}
