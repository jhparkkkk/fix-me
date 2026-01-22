package fixme.common.validation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for MessageValidator.validateSingleMessage() method.
 */
class MessageValidatorSingleMessageTest {
    
    // ========================================
    // VALID CASES (Should Pass)
    // ========================================
    
    @Test
    void testSingleCompleteMessage_ShouldPass() {
        String message = "49=B00001|56=M00001|35=D|55=AAPL|54=1|38=100|10=105|";
        
        ValidationResult result = MessageValidator.validateSingleMessage(message);
        
        assertTrue(result.isValid());
        assertNull(result.getErrorMessage());
    }
    
    @Test
    void testMinimalMessage_ShouldPass() {
        String message = "49=B00001|10=000|";
        
        ValidationResult result = MessageValidator.validateSingleMessage(message);
        
        assertTrue(result.isValid());
    }
    
    @Test
    void testMessageWithVariousChecksumValues_ShouldPass() {
        String[] messages = {
            "49=B00001|10=000|",
            "49=B00001|10=001|",
            "49=B00001|10=099|",
            "49=B00001|10=100|",
            "49=B00001|10=255|",
            "49=B00001|10=999|"
        };
        
        for (String message : messages) {
            ValidationResult result = MessageValidator.validateSingleMessage(message);
            assertTrue(result.isValid(), "Should be valid: " + message);
        }
    }
    
    // ========================================
    // INVALID CASES (Should Fail)
    // ========================================
    
    @Test
    void testNullMessage_ShouldFail() {
        ValidationResult result = MessageValidator.validateSingleMessage(null);
        
        assertFalse(result.isValid());
        assertEquals("Message is null or empty", result.getErrorMessage());
    }
    
    @Test
    void testEmptyMessage_ShouldFail() {
        ValidationResult result = MessageValidator.validateSingleMessage("");
        
        assertFalse(result.isValid());
        assertEquals("Message is null or empty", result.getErrorMessage());
    }
    
    @Test
    void testIncompleteMessage_NoChecksum_ShouldFail() {
        String incomplete = "49=B00001|56=M00001|35=D|55=AAPL|";
        
        ValidationResult result = MessageValidator.validateSingleMessage(incomplete);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("Incomplete message"));
        assertTrue(result.getErrorMessage().contains("missing checksum"));
    }
    
    @Test
    void testTwoMessages_ShouldFail() {
        String twoMessages = "49=B00001|10=105|49=B00002|10=106|";
        
        ValidationResult result = MessageValidator.validateSingleMessage(twoMessages);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("Multiple messages detected"));
        assertTrue(result.getErrorMessage().contains("2 messages"));
    }
    
    @Test
    void testThreeMessages_ShouldFail() {
        String threeMessages = "49=B00001|10=100|49=B00002|10=101|49=B00003|10=102|";
        
        ValidationResult result = MessageValidator.validateSingleMessage(threeMessages);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("Multiple messages detected"));
        assertTrue(result.getErrorMessage().contains("3 messages"));
    }
    
    @Test
    void testMessageWithTrailingGarbage_ShouldFail() {
        String messageWithGarbage = "49=B00001|10=105|GARBAGE";
        
        ValidationResult result = MessageValidator.validateSingleMessage(messageWithGarbage);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("Data after checksum"));
        assertTrue(result.getErrorMessage().contains("GARBAGE"));
    }
    
    @Test
    void testMessageWithPartialNextMessage_ShouldFail() {
        String partial = "49=B00001|10=105|49=B00002|56=M00001|";
        
        ValidationResult result = MessageValidator.validateSingleMessage(partial);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("Data after checksum"));
    }
    
    @Test
    void testInvalidChecksumFormat_TwoDigits_ShouldFail() {
        // Checksum must be exactly 3 digits
        String invalid = "49=B00001|10=05|";
        
        ValidationResult result = MessageValidator.validateSingleMessage(invalid);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("Incomplete message"));
    }
    
   
    
    @Test
    void testInvalidChecksumFormat_Letters_ShouldFail() {
        String invalid = "49=B00001|10=abc|";
        
        ValidationResult result = MessageValidator.validateSingleMessage(invalid);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("Incomplete message"));
    }
    
    // ========================================
    // EDGE CASES
    // ========================================
    
    @Test
    void testMessageEndingWithoutDelimiter_ShouldFail() {
        // Message doesn't end with | after checksum value
        String invalid = "49=B00001|10=105";
        
        ValidationResult result = MessageValidator.validateSingleMessage(invalid);
        
        assertFalse(result.isValid());
    }
    
    @Test
    void testOnlyChecksum_ShouldPass() {
        String onlyChecksum = "10=000|";
        
        ValidationResult result = MessageValidator.validateSingleMessage(onlyChecksum);
        
        assertTrue(result.isValid());
    }
    
    @Test
    void testMessageWithManyTags_OneChecksum_ShouldPass() {
        String longMessage = "49=B00001|56=M00001|35=D|55=AAPL|54=1|38=100|" +
                            "44=150.50|59=0|40=2|47=A|53=1|10=123|";
        
        ValidationResult result = MessageValidator.validateSingleMessage(longMessage);
        
        assertTrue(result.isValid());
    }
    
    @Test
    void testCompleteMessageFollowedByIncomplete_ShouldFail() {
        String mixed = "49=B00001|10=105|49=B00002|56=M00001|";
        
        ValidationResult result = MessageValidator.validateSingleMessage(mixed);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("Data after checksum"));
    }
}