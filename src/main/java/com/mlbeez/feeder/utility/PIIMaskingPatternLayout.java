package com.mlbeez.feeder.utility;


import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Custom Logback Pattern Layout that masks PII (Personally Identifiable Information)
 * Complies with CCPA/GDPR requirements for data protection in logs
 */
public class PIIMaskingPatternLayout extends PatternLayout {

    private Pattern multiPattern;

    private static final List<String> PII_PATTERNS = new ArrayList<>();

    static {
        // Email pattern
        PII_PATTERNS.add("([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})");

        // Phone number patterns (various formats)
        PII_PATTERNS.add("(\\d{3}[-\\.\\s]??\\d{3}[-\\.\\s]??\\d{4}|\\(\\d{3}\\)\\s*\\d{3}[-\\.\\s]??\\d{4}|\\d{3}[-\\.\\s]??\\d{4})");

        // Credit card patterns
        PII_PATTERNS.add("(\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4})");

        // SSN pattern
        PII_PATTERNS.add("(\\d{3}-\\d{2}-\\d{4})");

        // JWT tokens (Bearer tokens)
        PII_PATTERNS.add("(Bearer\\s+[A-Za-z0-9-_=]+\\.[A-Za-z0-9-_=]+\\.?[A-Za-z0-9-_.+/=]*)");

        // API keys / tokens (alphanumeric strings 20+ chars)
        PII_PATTERNS.add("([a-zA-Z0-9]{32,})");

        // Password patterns (password=, pwd=, pass=)
        PII_PATTERNS.add("(password|pwd|pass)\\s*[:=]\\s*[^\\s,;)]+");

        // OTP patterns (6 digit numbers in context)
        PII_PATTERNS.add("(?i)(otp|code)\\s*[:=]?\\s*(\\d{6})");
    }

    public PIIMaskingPatternLayout() {
        // Combine all patterns into one
        String patternString = PII_PATTERNS.stream()
                .collect(Collectors.joining("|"));
        this.multiPattern = Pattern.compile(patternString);
    }

    @Override
    public String doLayout(ILoggingEvent event) {
        String message = super.doLayout(event);
        return maskPII(message);
    }

    private String maskPII(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }

        Matcher matcher = multiPattern.matcher(message);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String matched = matcher.group();

            String replacement;
            if (matched.contains("@")) {
                // Email masking: keep first 2 chars and domain
                replacement = maskEmail(matched);
            } else if (matched.matches("\\d+")) {
                // Number masking (phone, SSN, credit card)
                if (matched.length() >= 6) {
                    replacement = matched.substring(0, 2) + "****" + matched.substring(matched.length() - 2);
                } else {
                    replacement = "******";
                }
            } else if (matched.toLowerCase().contains("password") || matched.toLowerCase().contains("pwd") || matched.toLowerCase().contains("pass")) {
                // Password field masking
                replacement = matched.split("[:=]")[0] + "=[REDACTED]";
            } else if (matched.toLowerCase().contains("bearer")) {
                // Token masking
                replacement = "Bearer [REDACTED_TOKEN]";
            } else if (matched.toLowerCase().contains("otp") || matched.toLowerCase().contains("code")) {
                // OTP masking
                replacement = matched.replaceAll("\\d{6}", "******");
            } else {
                // Generic masking for long alphanumeric strings
                if (matched.length() > 10) {
                    replacement = matched.substring(0, 4) + "****" + matched.substring(matched.length() - 4);
                } else {
                    replacement = "[REDACTED]";
                }
            }

            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "[REDACTED_EMAIL]";
        }

        String[] parts = email.split("@");
        String localPart = parts[0];
        String domain = parts[1];

        String maskedLocal;
        if (localPart.length() <= 2) {
            maskedLocal = "**";
        } else {
            maskedLocal = localPart.substring(0, 2) + "****";
        }

        return maskedLocal + "@" + domain;
    }
}

