package fixme.common.message;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeAll;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for FixMessageFactory
 */
class FixMessageFactoryTest {
    
    @BeforeAll
    static void setUp() {
        // Ensure FixConfig is loaded
        // The singleton will load the configuration from fix-tags.json
    }
    
    @Test
    @DisplayName("Should create valid buy order with all fields")
    void testCreateBuyOrderComplete() {
        FixMessage message = FixMessageFactory.createBuyOrder(
            "BROKER01",
            "MARKET01",
            "AAPL",
            100,
            150.50
        );
        
        assertNotNull(message, "Message should not be null");
        assertEquals("BROKER01", message.getSenderCompId(), "Sender should match");
        assertEquals("MARKET01", message.getTargetCompId(), "Target should match");
        assertEquals("D", message.getMsgType(), "Message type should be D (NewOrderSingle)");
        assertEquals("AAPL", message.getSymbol(), "Symbol should match");
        assertEquals("1", message.getField(FixTags.SIDE), "Side should be 1 (Buy)");
        assertEquals("100", message.getField(FixTags.ORDER_QTY), "Quantity should match");
        assertEquals("150.50", message.getField(FixTags.PRICE), "Price should match");
        assertTrue(message.isBuyOrder(), "Should be identified as buy order");
        assertNotNull(message.getField(FixTags.CHECKSUM), "Checksum should be present");
        assertTrue(message.isChecksumValid(), "Checksum should be valid");
    }
    
    @Test
    @DisplayName("Should create valid buy order without price")
    void testCreateBuyOrderWithoutPrice() {
        FixMessage message = FixMessageFactory.createBuyOrder(
            "BROKER01",
            "MARKET01",
            "GOOGL",
            50,
            null
        );
        
        assertNotNull(message);
        assertNull(message.getField(FixTags.PRICE), "Price should be null");
        assertTrue(message.isChecksumValid(), "Checksum should still be valid");
    }
    
    @Test
    @DisplayName("Should create valid sell order")
    void testCreateSellOrder() {
        FixMessage message = FixMessageFactory.createSellOrder(
            "BROKER02",
            "MARKET01",
            "GOOGL",
            50,
            2800.75
        );
        
        assertNotNull(message);
        assertEquals("BROKER02", message.getSenderCompId());
        assertEquals("MARKET01", message.getTargetCompId());
        assertTrue(message.isSellOrder(), "Should be identified as sell order");
        assertEquals("2", message.getField(FixTags.SIDE), "Side should be 2 (Sell)");
        assertEquals("50", message.getField(FixTags.ORDER_QTY));
        assertEquals("2800.75", message.getField(FixTags.PRICE));
        assertTrue(message.isChecksumValid());
    }
    
    @Test
    @DisplayName("Should create filled execution report")
    void testCreateFilledReport() {
        FixMessage message = FixMessageFactory.createFilledReport(
            "MARKET01",
            "BROKER01",
            "AAPL",
            100,
            150.50
        );
        
        assertNotNull(message);
        assertEquals("MARKET01", message.getSenderCompId());
        assertEquals("BROKER01", message.getTargetCompId());
        assertEquals("8", message.getMsgType(), "Message type should be 8 (ExecutionReport)");
        assertEquals("AAPL", message.getSymbol());
        assertEquals("2", message.getField(FixTags.ORD_STATUS), "Status should be 2 (Filled)");
        assertEquals("100", message.getField(FixTags.ORDER_QTY));
        assertEquals("150.50", message.getField(FixTags.PRICE));
        assertTrue(message.isExecutionReport(), "Should be identified as execution report");
        assertTrue(message.isChecksumValid());
    }
    
    @Test
    @DisplayName("Should create rejected execution report with text")
    void testCreateRejectedReport() {
        String rejectionReason = "Insufficient quantity available";
        
        FixMessage message = FixMessageFactory.createRejectedReport(
            "MARKET01",
            "BROKER01",
            "AAPL",
            rejectionReason
        );
        
        assertNotNull(message);
        assertEquals("MARKET01", message.getSenderCompId());
        assertEquals("BROKER01", message.getTargetCompId());
        assertEquals("8", message.getMsgType());
        assertEquals("AAPL", message.getSymbol());
        assertEquals("8", message.getField(FixTags.ORD_STATUS), "Status should be 8 (Rejected)");
        assertEquals(rejectionReason, message.getField(FixTags.TEXT));
        assertNull(message.getField(FixTags.ORDER_QTY), "Quantity should be null for rejected");
        assertNull(message.getField(FixTags.PRICE), "Price should be null for rejected");
        assertTrue(message.isChecksumValid());
    }
    
    @Test
    @DisplayName("Should parse message from string successfully")
    void testFromStringValid() {
        // Create a message first
        FixMessage original = FixMessageFactory.createBuyOrder(
            "BROKER01",
            "MARKET01",
            "AAPL",
            100,
            150.50
        );
        
        // Convert to string and parse back
        String rawMessage = original.toString();
        System.out.println("Raw message: " + rawMessage);
        
        FixMessage parsed = FixMessageFactory.fromString(rawMessage);
        
        assertNotNull(parsed);
        assertEquals(original.getSenderCompId(), parsed.getSenderCompId());
        assertEquals(original.getTargetCompId(), parsed.getTargetCompId());
        assertEquals(original.getMsgType(), parsed.getMsgType());
        assertEquals(original.getSymbol(), parsed.getSymbol());
        assertEquals(original.getField(FixTags.SIDE), parsed.getField(FixTags.SIDE));
        assertEquals(original.getField(FixTags.ORDER_QTY), parsed.getField(FixTags.ORDER_QTY));
        assertEquals(original.getField(FixTags.PRICE), parsed.getField(FixTags.PRICE));
        assertTrue(parsed.isChecksumValid());
    }
    
    @Test
    @DisplayName("Should throw exception for null raw message")
    void testFromStringNull() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> FixMessageFactory.fromString(null)
        );
        
        assertTrue(exception.getMessage().contains("cannot be null"));
    }
    
    @Test
    @DisplayName("Should throw exception for empty raw message")
    void testFromStringEmpty() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> FixMessageFactory.fromString("")
        );
        
        assertTrue(exception.getMessage().contains("cannot be null or empty"));
    }
    
    @Test
    @DisplayName("Should throw exception for message with invalid checksum")
    void testFromStringInvalidChecksum() {
        // Create a message with intentionally wrong checksum
        String invalidMessage = "49=BROKER01|56=MARKET01|35=D|55=AAPL|54=1|38=100|44=150.50|10=999|";
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> FixMessageFactory.fromString(invalidMessage)
        );
        
        assertTrue(exception.getMessage().contains("checksum"));
    }
    
    @Test
    @DisplayName("Should throw exception for message missing required tags")
    void testValidationMissingRequiredTag() {
        // Try to create a message manually without required fields
        // This test verifies that validation catches missing required tags
        
        // We can't directly test this with the factory methods since they ensure
        // all required fields are present, but we can test the fromString validation
        
        // First, create a valid message and calculate its checksum
        FixMessage tempMessage = new FixMessage();
        tempMessage.setField(FixTags.SENDER_COMP_ID, "BROKER01");
        tempMessage.setField(FixTags.TARGET_COMP_ID, "MARKET01");
        tempMessage.setField(FixTags.MSG_TYPE, "D");
        tempMessage.setField(FixTags.SIDE, "1");
        tempMessage.setField(FixTags.ORDER_QTY, "100");
        // Note: Symbol (55) is intentionally missing
        
        String checksum = tempMessage.calculateChecksum();
        
        String incompleteMessage = "49=BROKER01|56=MARKET01|35=D|54=1|38=100|10=" + checksum + "|";
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> FixMessageFactory.fromString(incompleteMessage)
        );
        
        assertTrue(exception.getMessage().contains("Required tag") || 
                   exception.getMessage().contains("missing"),
                   "Expected error about missing required tag, got: " + exception.getMessage());
    }
    
    @Test
    @DisplayName("Should create execution report with all optional fields")
    void testCreateExecutionReportComplete() {
        FixMessage message = FixMessageFactory.createExecutionReport(
            "MARKET01",
            "BROKER01",
            "TSLA",
            FixTags.ORD_STATUS_FILLED,
            75,
            250.25,
            "Order executed successfully"
        );
        
        assertNotNull(message);
        assertEquals("TSLA", message.getSymbol());
        assertEquals("2", message.getField(FixTags.ORD_STATUS));
        assertEquals("75", message.getField(FixTags.ORDER_QTY));
        assertEquals("250.25", message.getField(FixTags.PRICE));
        assertEquals("Order executed successfully", message.getField(FixTags.TEXT));
        assertTrue(message.isChecksumValid());
    }
    
    @Test
    @DisplayName("Should handle price formatting correctly")
    void testPriceFormatting() {
        FixMessage message1 = FixMessageFactory.createBuyOrder(
            "BROKER01",
            "MARKET01",
            "AAPL",
            100,
            150.5
        );
        
        assertEquals("150.50", message1.getField(FixTags.PRICE), 
                     "Price should be formatted with 2 decimal places");
        
        FixMessage message2 = FixMessageFactory.createBuyOrder(
            "BROKER01",
            "MARKET01",
            "AAPL",
            100,
            150.123
        );
        
        assertEquals("150.12", message2.getField(FixTags.PRICE), 
                     "Price should be rounded to 2 decimal places");
    }
    
    @Test
    @DisplayName("Should create NewOrderSingle using generic method")
    void testCreateNewOrderSingleGeneric() {
        FixMessage buyMessage = FixMessageFactory.createNewOrderSingle(
            "BROKER01",
            "MARKET01",
            "AAPL",
            FixTags.SIDE_BUY,
            100,
            150.50
        );
        
        assertTrue(buyMessage.isBuyOrder());
        
        FixMessage sellMessage = FixMessageFactory.createNewOrderSingle(
            "BROKER01",
            "MARKET01",
            "AAPL",
            FixTags.SIDE_SELL,
            100,
            150.50
        );
        
        assertTrue(sellMessage.isSellOrder());
    }
    
    @Test
    @DisplayName("Should verify checksum calculation consistency")
    void testChecksumConsistency() {
        FixMessage message1 = FixMessageFactory.createBuyOrder(
            "BROKER01",
            "MARKET01",
            "AAPL",
            100,
            150.50
        );
        
        FixMessage message2 = FixMessageFactory.createBuyOrder(
            "BROKER01",
            "MARKET01",
            "AAPL",
            100,
            150.50
        );
        
        assertEquals(message1.getField(FixTags.CHECKSUM), 
                     message2.getField(FixTags.CHECKSUM),
                     "Identical messages should have identical checksums");
    }
    
    @Test
    @DisplayName("Should handle round-trip serialization correctly")
    void testRoundTripSerialization() {
        FixMessage original = FixMessageFactory.createExecutionReport(
            "MARKET01",
            "BROKER01",
            "AAPL",
            FixTags.ORD_STATUS_FILLED,
            100,
            150.50,
            "Test message"
        );
        
        String serialized = original.toString();
        FixMessage deserialized = FixMessageFactory.fromString(serialized);
        
        assertEquals(original.getSenderCompId(), deserialized.getSenderCompId());
        assertEquals(original.getTargetCompId(), deserialized.getTargetCompId());
        assertEquals(original.getMsgType(), deserialized.getMsgType());
        assertEquals(original.getSymbol(), deserialized.getSymbol());
        assertEquals(original.getField(FixTags.ORD_STATUS), 
                     deserialized.getField(FixTags.ORD_STATUS));
        assertEquals(original.getField(FixTags.ORDER_QTY), 
                     deserialized.getField(FixTags.ORDER_QTY));
        assertEquals(original.getField(FixTags.PRICE), 
                     deserialized.getField(FixTags.PRICE));
        assertEquals(original.getField(FixTags.TEXT), 
                     deserialized.getField(FixTags.TEXT));
        assertEquals(original.getField(FixTags.CHECKSUM), 
                     deserialized.getField(FixTags.CHECKSUM));
    }
}