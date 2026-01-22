package fixme.common.validation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for MessageValidator - Format validation
 */
@DisplayName("MessageValidator - Format Validation Tests")
class MessageValidatorTest {
    
    // ========================================
    // VALID MESSAGES
    // ========================================
    
    @Test
    @DisplayName("Valid complete message should pass")
    void testValidCompleteMessage() {
        String validMessage = "49=B00001|56=M00001|35=D|55=AAPL|54=1|38=100|10=105|";
        
        ValidationResult result = MessageValidator.validate(validMessage);
        
        assertTrue(result.isValid(), "Valid message should pass validation");
        assertNull(result.getErrorMessage(), "Valid message should have no error");
    }
    
    @Test
    @DisplayName("Minimal valid message should pass")
    void testMinimalValidMessage() {
        String minimal = "49=B00001|10=000|";
        
        ValidationResult result = MessageValidator.validate(minimal);
        
        assertTrue(result.isValid());
    }
    
    @Test
    @DisplayName("Message with many tags should pass")
    void testMessageWithManyTags() {
        String message = "49=B00001|56=M00001|35=D|55=AAPL|54=1|38=100|" +
                        "44=150.50|59=0|40=2|47=A|53=1|10=123|";
        
        ValidationResult result = MessageValidator.validate(message);
        
        assertTrue(result.isValid());
    }
    
    // ========================================
    // NULL / EMPTY CHECKS
    // ========================================
    
    @Test
    @DisplayName("Null message should fail")
    void testNullMessage() {
        ValidationResult result = MessageValidator.validate(null);
        
        assertFalse(result.isValid());
        assertEquals("Message is null or empty", result.getErrorMessage());
    }
    
    @Test
    @DisplayName("Empty message should fail")
    void testEmptyMessage() {
        ValidationResult result = MessageValidator.validate("");
        
        assertFalse(result.isValid());
        assertEquals("Message is null or empty", result.getErrorMessage());
    }
    
    // ========================================
    // FORMAT ERRORS
    // ========================================
    
    @Test
    @DisplayName("Message without '=' should fail")
    void testMessageWithoutEquals() {
        String invalid = "49B00001|56M00001|";
        
        ValidationResult result = MessageValidator.validate(invalid);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("missing '='"));
    }
    
    @Test
    @DisplayName("Message without delimiter should fail")
    void testMessageWithoutDelimiter() {
        String invalid = "49=B00001 56=M00001";
        
        ValidationResult result = MessageValidator.validate(invalid);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("missing delimiter"));
    }
    
    @Test
    @DisplayName("Message with double delimiter should fail")
    void testMessageWithDoubleDelimiter() {
        String invalid = "49=B00001||56=M00001|";
        
        ValidationResult result = MessageValidator.validate(invalid);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("double delimiter"));
    }
    
    @Test
    @DisplayName("Message not ending with delimiter should fail")
    void testMessageNotEndingWithDelimiter() {
        String invalid = "49=B00001|56=M00001";
        
        ValidationResult result = MessageValidator.validate(invalid);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("must end with"));
    }
    
    // ========================================
    // TAG ERRORS
    // ========================================
    
    @Test
    @DisplayName("Empty tag should fail")
    void testEmptyTag() {
        String invalid = "=B00001|56=M00001|";
        
        ValidationResult result = MessageValidator.validate(invalid);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("Empty tag"));
    }
    
    @Test
    @DisplayName("Non-numeric tag should fail")
    void testNonNumericTag() {
        String invalid = "ABC=B00001|56=M00001|";
        
        ValidationResult result = MessageValidator.validate(invalid);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("Non-numeric tag"));
    }
    
    @Test
    @DisplayName("Empty value should fail")
    void testEmptyValue() {
        String invalid = "49=|56=M00001|";
        
        ValidationResult result = MessageValidator.validate(invalid);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("Empty value"));
    }
    
    @Test
    @DisplayName("Duplicate tag should fail")
    void testDuplicateTag() {
        String invalid = "49=B00001|56=M00001|49=B00002|";
        
        ValidationResult result = MessageValidator.validate(invalid);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("Duplicate tag: 49"));
    }
    
    // ========================================
    // SECURITY CHECKS
    // ========================================
    
    @Test
    @DisplayName("Message too long should fail (DOS protection)")
    void testMessageTooLong() {
        // Create a message > 4096 bytes
        StringBuilder longMessage = new StringBuilder("49=B00001|");
        for (int i = 0; i < 500; i++) {
            longMessage.append("56=VERYLONGVALUE|");
        }
        
        ValidationResult result = MessageValidator.validate(longMessage.toString());
        
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("too long"));
    }
    
    @Test
    @DisplayName("Too many tags should fail")
    void testTooManyTags() {
        // Create message with > 50 tags
        StringBuilder manyTags = new StringBuilder();
        for (int i = 1; i <= 60; i++) {
            manyTags.append(i).append("=VALUE").append(i).append("|");
        }
        
        ValidationResult result = MessageValidator.validate(manyTags.toString());
        
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("Too many tags"));
    }
    
    @Test
    @DisplayName("Dangerous characters should fail (injection protection)")
    void testDangerousCharacters() {
        String[] dangerousValues = {
            "49=<script>|56=M00001|",
            "49=B00001|56=M'00001|",
            "49=B00001|56=M\"00001|",
            "49=B00001|56=M;DROP|",
            "49=B00001|56=M\\00001|"
        };
        
        for (String dangerous : dangerousValues) {
            ValidationResult result = MessageValidator.validate(dangerous);
            
            assertFalse(result.isValid(), 
                       "Message with dangerous chars should fail: " + dangerous);
            assertTrue(result.getErrorMessage().contains("Invalid characters"));
        }
    }
    
    @Test
    @DisplayName("Value too long should fail")
    void testValueTooLong() {
        // Create value > 512 bytes
        StringBuilder longValue = new StringBuilder();
        for (int i = 0; i < 600; i++) {
            longValue.append("X");
        }
        
        String invalid = "49=" + longValue.toString() + "|56=M00001|";
        
        ValidationResult result = MessageValidator.validate(invalid);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("Value too long"));
    }
    
    // ========================================
    // EDGE CASES
    // ========================================
    
    @Test
    @DisplayName("Tag with multiple '=' should be handled correctly")
    void testTagWithMultipleEquals() {
        String invalid = "49==B00001|56=M00001|";
        
        ValidationResult result = MessageValidator.validate(invalid);
        
        // Empty tag before first '='
        assertFalse(result.isValid());
    }
    
    @Test
    @DisplayName("Only delimiter should fail")
    void testOnlyDelimiter() {
        String invalid = "|";
        
        ValidationResult result = MessageValidator.validate(invalid);
        
        assertFalse(result.isValid());
    }
    
    // ========================================
    // QUICK VALIDATION
    // ========================================
    
    @Test
    @DisplayName("isValidQuick should accept valid message")
    void testIsValidQuick_Valid() {
        String valid = "49=B00001|56=M00001|10=105|";
        
        assertTrue(MessageValidator.isValidQuick(valid));
    }
    
    @Test
    @DisplayName("isValidQuick should reject null")
    void testIsValidQuick_Null() {
        assertFalse(MessageValidator.isValidQuick(null));
    }
    
    @Test
    @DisplayName("isValidQuick should reject too long")
    void testIsValidQuick_TooLong() {
        StringBuilder longMsg = new StringBuilder();
        for (int i = 0; i < 5000; i++) {
            longMsg.append("X");
        }
        
        assertFalse(MessageValidator.isValidQuick(longMsg.toString()));
    }
}