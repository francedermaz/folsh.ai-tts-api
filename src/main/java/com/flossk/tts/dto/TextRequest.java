package com.flossk.tts.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request containing text input")
public class TextRequest {

    @NotBlank(message = "Text is required")
    @Schema(description = "Text to process", required = true, example = "Prodhimi 100 MWh")
    private String text;

    public TextRequest() {
    }

    public TextRequest(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
