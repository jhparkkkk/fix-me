package fixme.common.validation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
/**
 * Validates raw FIX messages for format correctness and security.
 * Protects against malformed messages, DOS attacks, and injection attempts.
 * 
 * Design Pattern: Strategy + Fail-Fast
 */
public class MessageValidator {
    
    private static final Logger logger = LoggerFactory.getLogger(MessageValidator.class);
    
    // Security limits
    private static final int MAX_MESSAGE_LENGTH = 4096;      // 4KB max per message
    private static final int MAX_TAG_VALUE_LENGTH = 512;     // 512 bytes max per value
    private static final int MAX_TAGS_PER_MESSAGE = 50;      // Max 50 tags
    private static final Pattern TAG_PATTERN = Pattern.compile("^\\d+$");  // Tags must be numeric
    
    // Dangerous characters (for injection protection)
    private static final Pattern DANGEROUS_CHARS = Pattern.compile("[<>'\";\\\\]");
    private static final Pattern CHECKSUM_PATTERN = Pattern.compile("10=\\d{3}\\|");

    private MessageValidator() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static ValidationResult validateSingleMessage(String rawMessage) {
        if (rawMessage == null || rawMessage.isEmpty()) {
            return ValidationResult.fail("Message is null or empty");
        }
        
        java.util.regex.Matcher matcher = CHECKSUM_PATTERN.matcher(rawMessage);
        
        int checksumCount = 0;
        int lastChecksumEnd = 0;
        
        while (matcher.find()) {
            checksumCount++;
            lastChecksumEnd = matcher.end();
        }
        logger.debug("Checksum count found: {}", checksumCount);        
        if (checksumCount == 0) {
            logger.debug("No checksum found in message");
            return ValidationResult.fail(
                "Incomplete message: missing checksum (tag 10)"
            );
        }
        
        if (checksumCount > 1) {
            logger.debug("Multiple checksums found: {}", checksumCount);
            return ValidationResult.fail(
                String.format("Multiple messages detected (%d messages in buffer). " +
                             "Send one message at a time.", checksumCount)
            );
        }
        
        if (lastChecksumEnd < rawMessage.length()) {
            String trailingData = rawMessage.substring(lastChecksumEnd);
            logger.debug("Trailing data after checksum: '{}'", trailingData);
            return ValidationResult.fail(
                String.format("Data after checksum: '%s'. Message must end with checksum.", 
                             trailingData)
            );
        }
        
        logger.debug("Single message validation passed");
        return ValidationResult.success();
    }

    
    
    /**
     * Validates a raw FIX message before parsing.
     * Checks format, length limits, and security constraints.
     * 
     * @param rawMessage The raw message to validate
     * @return ValidationResult with success/failure and error message
     */
    public static ValidationResult validate(String rawMessage) {
        logger.debug("Validating message: {} chars", rawMessage != null ? rawMessage.length() : 0);
        
        // 1. Null/empty check
        if (rawMessage == null || rawMessage.isEmpty()) {
            return ValidationResult.fail("Message is null or empty");
        }
        
        // 2. Length check (DOS protection)
        if (rawMessage.length() > MAX_MESSAGE_LENGTH) {
            logger.warn("Message too long: {} > {}", rawMessage.length(), MAX_MESSAGE_LENGTH);
            return ValidationResult.fail(
                String.format("Message too long: %d bytes (max: %d)", 
                             rawMessage.length(), MAX_MESSAGE_LENGTH)
            );
        }
        
        // 3. Basic format check
        if (!rawMessage.contains("=")) {
            return ValidationResult.fail("Invalid format: missing '='");
        }
        
        if (!rawMessage.contains("|")) {
            return ValidationResult.fail("Invalid format: missing delimiter '|'");
        }
        
        // 4. Check for double delimiters (||)
        if (rawMessage.contains("||")) {
            return ValidationResult.fail("Invalid format: double delimiter '||'");
        }
        
        // 5. Must end with delimiter
        if (!rawMessage.endsWith("|")) {
            return ValidationResult.fail("Invalid format: message must end with '|'");
        }
        
        // 6. Parse and validate each tag
        String[] parts = rawMessage.split("\\|");
        
        if (parts.length > MAX_TAGS_PER_MESSAGE) {
            return ValidationResult.fail(
                String.format("Too many tags: %d (max: %d)", 
                             parts.length, MAX_TAGS_PER_MESSAGE)
            );
        }
        
        Set<String> seenTags = new HashSet<>();
        
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;  // Last part after final | is empty
            }

            long equalsCount = part.chars().filter(ch -> ch == '=').count();
            if (equalsCount != 1) {
                return ValidationResult.fail(
                    String.format("Invalid tag format: '%s' (expected exactly one '=' per tag)", part)
                );
            }
            
            
            // 7. Each part must have exactly one '='
            String[] keyValue = part.split("=", 2);
            if (keyValue.length != 2) {
                return ValidationResult.fail(
                    String.format("Invalid tag format: '%s' (expected 'tag=value')", part)
                );
            }
            
            String tag = keyValue[0];
            String value = keyValue[1];
            
            // 8. Tag must not be empty
            if (tag.isEmpty()) {
                return ValidationResult.fail("Empty tag found");
            }
            
            // 9. Tag must be numeric
            if (!TAG_PATTERN.matcher(tag).matches()) {
                return ValidationResult.fail(
                    String.format("Non-numeric tag: '%s' (tags must be numbers)", tag)
                );
            }
            
            // 10. Value must not be empty (except for specific cases)
            if (value.isEmpty()) {
                return ValidationResult.fail(
                    String.format("Empty value for tag %s", tag)
                );
            }
            
            // 11. Value length check
            if (value.length() > MAX_TAG_VALUE_LENGTH) {
                logger.warn("Value too long for tag {}: {} > {}", 
                           tag, value.length(), MAX_TAG_VALUE_LENGTH);
                return ValidationResult.fail(
                    String.format("Value too long for tag %s: %d bytes (max: %d)", 
                                 tag, value.length(), MAX_TAG_VALUE_LENGTH)
                );
            }
            
            // 12. Check for duplicate tags
            if (seenTags.contains(tag)) {
                return ValidationResult.fail(
                    String.format("Duplicate tag: %s", tag)
                );
            }
            seenTags.add(tag);
            
            // 13. Check for dangerous characters (injection protection)
            if (DANGEROUS_CHARS.matcher(value).find()) {
                logger.warn("Dangerous characters in tag {}: {}", tag, value);
                return ValidationResult.fail(
                    String.format("Invalid characters in tag %s value", tag)
                );
            }
        }
        
        logger.debug("Message validation successful: {} tags", seenTags.size());
        return ValidationResult.success();
    }
    
    /**
     * Quick validation for high-throughput scenarios.
     * Only checks critical security constraints.
     */
    public static boolean isValidQuick(String rawMessage) {
        return rawMessage != null && 
               rawMessage.length() <= MAX_MESSAGE_LENGTH &&
               rawMessage.contains("=") &&
               rawMessage.contains("|") &&
               rawMessage.endsWith("|");
    }

    


    
}