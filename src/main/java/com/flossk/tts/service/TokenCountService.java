package com.flossk.tts.service;

import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
public class TokenCountService {
    
    /**
     * Counts the number of words longer than 3 characters in the given text.
     * Words are split by whitespace and punctuation is removed.
     * 
     * @param text The text to analyze
     * @return The count of words longer than 3 characters
     */
    public int countTokens(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        
        // Split by whitespace and filter out empty strings
        String[] words = text.trim().split("\\s+");
        
        return (int) Arrays.stream(words)
            .map(word -> word.replaceAll("[^\\p{L}\\p{N}]", "")) // Remove punctuation, keep letters and numbers
            .filter(word -> word.length() > 3)
            .count();
    }
}
