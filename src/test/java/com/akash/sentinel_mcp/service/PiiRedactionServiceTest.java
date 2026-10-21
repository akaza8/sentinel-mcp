package com.akash.sentinel_mcp.service;

import com.akash.sentinel.service.PiiRedactionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PiiRedactionServiceTest {

    private final PiiRedactionService service = new PiiRedactionService();

    @Test
    @DisplayName("Redacts standard Bearer / JWT tokens")
    void redactsJwtTokens() {
        String input = "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
        String result = service.redact(input);
        
        assertThat(result).contains("[REDACTED_JWT]");
        assertThat(result).doesNotContain("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9");
    }

    @Test
    @DisplayName("Redacts multiple JWT tokens in same message")
    void redactsMultipleJwts() {
        String input = "Token1: eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0In0.abc123 Token2: eyJhbGciOiJSUzI1NiJ9.eyJ1c2VyIjoidGVzdCJ9.xyz789";
        String result = service.redact(input);
        
        assertThat(result).contains("[REDACTED_JWT]");
        assertThat(result).doesNotContain("eyJhbGciOiJIUzI1NiJ9");
        assertThat(result).doesNotContain("eyJhbGciOiJSUzI1NiJ9");
    }

    @Test
    @DisplayName("Redacts API keys with various formats")
    void redactsApiKeys() {
        String input1 = "Config: api_key=abcdef1234567890abcdef1234567890";
        String input2 = "Settings: apiKey: sk_live_1234567890abcdef";
        String input3 = "Auth: token=\"ghp_1234567890abcdefghijklmnop\"";
        String input4 = "Credentials: secret = xoxb-1234567890-abcdefghijklmnop";
        
        assertThat(service.redact(input1)).contains("[REDACTED_KEY]").doesNotContain("abcdef1234567890abcdef1234567890");
        assertThat(service.redact(input2)).contains("[REDACTED_KEY]").doesNotContain("sk_live_1234567890abcdef");
        assertThat(service.redact(input3)).contains("[REDACTED_KEY]").doesNotContain("ghp_1234567890abcdefghijklmnop");
        assertThat(service.redact(input4)).contains("[REDACTED_KEY]").doesNotContain("xoxb-1234567890-abcdefghijklmnop");
    }

    @Test
    @DisplayName("Redacts email addresses")
    void redactsEmails() {
        String input = "User john.doe@example.com logged in, contacted support@company.org";
        String result = service.redact(input);
        
        assertThat(result).contains("[REDACTED_EMAIL]");
        assertThat(result).doesNotContain("john.doe@example.com");
        assertThat(result).doesNotContain("support@company.org");
    }

    @Test
    @DisplayName("Redacts credit card numbers with various formats")
    void redactsCreditCards() {
        String input1 = "Payment with card 4111111111111111";
        String input2 = "Card: 5500-0000-0000-0004";
        String input3 = "CC: 3782 822463 10005";
        
        assertThat(service.redact(input1)).contains("[REDACTED_CC]").doesNotContain("4111111111111111");
        assertThat(service.redact(input2)).contains("[REDACTED_CC]").doesNotContain("5500-0000-0000-0004");
        assertThat(service.redact(input3)).contains("[REDACTED_CC]").doesNotContain("3782 822463 10005");
    }

    @Test
    @DisplayName("Leaves logs with no PII completely untouched")
    void leavesNonPiiUntouched() {
        String input = "System started successfully. Processing order 12345. No errors found.";
        String result = service.redact(input);
        
        assertThat(result).isEqualTo(input);
    }

    @Test
    @DisplayName("Handles empty strings")
    void handlesEmptyStrings() {
        String result = service.redact("");
        
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Handles null inputs")
    void handlesNullInputs() {
        String result = service.redact(null);
        
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Redacts multiple PII entities in a single log message")
    void redactsMultiplePiiEntities() {
        String input = "User john.doe@example.com authenticated with api_key=abcdef1234567890abcdef1234567890 and processed payment for card 4111111111111111 using token eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0In0.abc123";
        String result = service.redact(input);
        
        assertThat(result).contains("[REDACTED_EMAIL]");
        assertThat(result).contains("[REDACTED_KEY]");
        assertThat(result).contains("[REDACTED_CC]");
        assertThat(result).contains("[REDACTED_JWT]");
        assertThat(result).doesNotContain("john.doe@example.com");
        assertThat(result).doesNotContain("abcdef1234567890abcdef1234567890");
        assertThat(result).doesNotContain("4111111111111111");
        assertThat(result).doesNotContain("eyJhbGciOiJIUzI1NiJ9");
    }

    @Test
    @DisplayName("Preserves non-sensitive numbers and text")
    void preservesNonSensitiveContent() {
        String input = "Order 12345 processed at 2024-05-20 for amount 99.99";
        String result = service.redact(input);
        
        assertThat(result).isEqualTo(input);
        assertThat(result).contains("12345");
        assertThat(result).contains("99.99");
    }
}
