package com.flossk.tts.dto;

import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ApiRequestDeserializationTest {

    private static final String MULTILINE_SPEAK_JSON = """
        {
          "text": "Konsumi i rrymës në tetor u rrit për 17%

        Konsumi i energjisë elektrike",
          "voiceId": "arta"
        }
        """;

    private static final String MULTILINE_NORMALIZE_JSON = """
        {
          "text": "Konsumi i rrymës në tetor u rrit për 17%

        Konsumi i energjisë elektrike"
        }
        """;

    @Test
    void rejectsMultilineJsonWithoutFeatureForSpeakRequest() {
        ObjectMapper mapper = new ObjectMapper();
        assertThrows(Exception.class, () -> mapper.readValue(MULTILINE_SPEAK_JSON, SpeakRequest.class));
    }

    @Test
    void rejectsMultilineJsonWithoutFeatureForNormalizeRequest() {
        ObjectMapper mapper = new ObjectMapper();
        assertThrows(Exception.class, () -> mapper.readValue(MULTILINE_NORMALIZE_JSON, NormalizeRequest.class));
    }

    @Test
    void acceptsMultilineJsonWithUnescapedControlCharsForSpeakRequest() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS.mappedFeature(), true);

        SpeakRequest request = mapper.readValue(MULTILINE_SPEAK_JSON, SpeakRequest.class);

        assertNotNull(request.getText());
        assertEquals("arta", request.getVoiceId());
    }

    @Test
    void acceptsMultilineJsonWithUnescapedControlCharsForNormalizeRequest() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS.mappedFeature(), true);

        NormalizeRequest request = mapper.readValue(MULTILINE_NORMALIZE_JSON, NormalizeRequest.class);

        assertNotNull(request.getText());
    }

    @Test
    void jackson3AcceptsMultilineJsonForNormalizeRequest() throws Exception {
        JsonMapper mapper = JsonMapper.builder()
            .enable(tools.jackson.core.json.JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS)
            .build();

        NormalizeRequest request = mapper.readValue(MULTILINE_NORMALIZE_JSON, NormalizeRequest.class);

        assertNotNull(request.getText());
    }

    @Test
    void acceptsEscapedNewlinesForBothRequestTypes() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String json = "{\"text\": \"line one\\n\\nline two\", \"voiceId\": \"arta\"}";

        SpeakRequest speakRequest = mapper.readValue(json, SpeakRequest.class);
        NormalizeRequest normalizeRequest = mapper.readValue(
            "{\"text\": \"line one\\n\\nline two\"}",
            NormalizeRequest.class
        );

        assertEquals("line one\n\nline two", speakRequest.getText());
        assertEquals("line one\n\nline two", normalizeRequest.getText());
    }
}
