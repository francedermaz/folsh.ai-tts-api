package com.flossk.tts.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request to generate speech from text")
public class SpeakRequest {
    
    @NotBlank(message = "Text is required")
    @Schema(description = "Text to convert to speech", required = true, example = "Hello, world!")
    private String text;
    
    @NotBlank(message = "Voice ID is required")
    @Schema(description = "Voice model ID (edon, arta, arben, or dren)", required = true, example = "edon")
    private String voiceId;
    
    public SpeakRequest() {
    }
    
    public SpeakRequest(String text, String voiceId) {
        this.text = text;
        this.voiceId = voiceId;
    }
    
    public String getText() {
        return text;
    }
    
    public void setText(String text) {
        this.text = text;
    }
    
    public String getVoiceId() {
        return voiceId;
    }
    
    public void setVoiceId(String voiceId) {
        this.voiceId = voiceId;
    }
}
