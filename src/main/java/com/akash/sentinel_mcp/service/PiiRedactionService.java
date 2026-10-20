package com.akash.sentinel_mcp.service;

import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
public class PiiRedactionService {

    private static final Pattern JWT_PATTERN = Pattern.compile("eyJ[A-Za-z0-9_-]*\\.eyJ[A-Za-z0-9_-]*\\.[A-Za-z0-9_-]*");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
    private static final Pattern API_KEY_PATTERN = Pattern.compile("(api[_-]?key|token|secret)[\\s]*[=:][\\s]*[\"']?[a-zA-Z0-9_\\-]{16,}[\"']?", Pattern.CASE_INSENSITIVE);
    private static final Pattern CC_PATTERN = Pattern.compile("\\b(?:\\d[ -]*?){13,16}\\b");

    public String redact(String input) {
        if (input == null) return null;
        String s = JWT_PATTERN.matcher(input).replaceAll("[REDACTED_JWT]");
        s = EMAIL_PATTERN.matcher(s).replaceAll("[REDACTED_EMAIL]");
        s = API_KEY_PATTERN.matcher(s).replaceAll("$1=[REDACTED_KEY]");
        s = CC_PATTERN.matcher(s).replaceAll("[REDACTED_CC]");
        return s;
    }
}
