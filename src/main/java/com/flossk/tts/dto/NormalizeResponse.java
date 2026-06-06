package com.flossk.tts.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Normalized text ready for TTS")
public class NormalizeResponse {

    @Schema(description = "Normalized text", example = "Prodhimi 100 megavat")
    private String text;

    public NormalizeResponse() {
    }

    public NormalizeResponse(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
