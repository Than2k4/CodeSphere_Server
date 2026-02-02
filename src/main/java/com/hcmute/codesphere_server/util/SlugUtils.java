package com.hcmute.codesphere_server.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public class SlugUtils {
    
    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");
    private static final Pattern EDGESDASHES = Pattern.compile("(^-|-$)");

    /**
     * Generate slug from text
     * Example: "Java Programming" -> "java-programming"
     */
    public static String generateSlug(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "";
        }
        
        // Normalize to NFD (decompose accented characters)
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        
        // Remove accents
        normalized = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        
        // Convert to lowercase
        normalized = normalized.toLowerCase(Locale.ENGLISH);
        
        // Replace whitespace with dashes
        normalized = WHITESPACE.matcher(normalized).replaceAll("-");
        
        // Remove non-latin characters
        normalized = NONLATIN.matcher(normalized).replaceAll("");
        
        // Remove dashes from edges
        normalized = EDGESDASHES.matcher(normalized).replaceAll("");
        
        // Replace multiple dashes with single dash
        normalized = normalized.replaceAll("-+", "-");
        
        return normalized;
    }
}

