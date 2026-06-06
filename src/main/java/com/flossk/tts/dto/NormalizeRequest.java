package com.flossk.tts.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request to normalize text for TTS")
public class NormalizeRequest extends TextRequest {

    public NormalizeRequest() {
    }

    public NormalizeRequest(String text) {
        super(text);
    }
}
