package fixme.common.message;

import fixme.common.config.FixConfig;

/**
 * Test complet de FixMessage et FixTags
 */
public class FixMessageTest {
    
    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════════╗");
        System.out.println("║    FIX MESSAGE TEST SUITE                 ║");
        System.out.println("╚════════════════════════════════════════════╝\n");
        
        try {
            testFixTags();
            testFixMessageCreation();
            testFixMessageGettersSetters();
            testFixMessageTypes();
            testChecksumCalculation();
            testChecksumValidation();
            testMessageParsing();
            testMessageSerialization();
            
            System.out.println("\n╔════════════════════════════════════════════╗");
            System.out.println("║    ✅ ALL TESTS PASSED!                   ║");
            System.out.println("╚════════════════════════════════════════════╝");
            
        } catch (Exception e) {
            System.err.println("\n❌ TEST FAILED!");
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    /**
     * Test 1 : Constantes FixTags
     */
    private static void testFixTags() {
        System.out.println("━━━ Test 1: FixTags Constants ━━━");
        
        // Vérifier que les constantes sont définies
        assert FixTags.SENDER_COMP_ID.equals("49") : "SENDER_COMP_ID should be 49";
        assert FixTags.TARGET_COMP_ID.equals("56") : "TARGET_COMP_ID should be 56";
        assert FixTags.MSG_TYPE.equals("35") : "MSG_TYPE should be 35";
        assert FixTags.CHECKSUM.equals("10") : "CHECKSUM should be 10";
        
        System.out.println("✓ System tags: 49, 56, 35, 10");
        
        assert FixTags.SYMBOL.equals("55") : "SYMBOL should be 55";
        assert FixTags.SIDE.equals("54") : "SIDE should be 54";
        assert FixTags.ORDER_QTY.equals("38") : "ORDER_QTY should be 38";
        assert FixTags.PRICE.equals("44") : "PRICE should be 44";
        
        System.out.println("✓ Order tags: 55, 54, 38, 44");
        
        assert FixTags.ORD_STATUS.equals("39") : "ORD_STATUS should be 39";
        assert FixTags.TEXT.equals("58") : "TEXT should be 58";
        
        System.out.println("✓ Execution tags: 39, 58");
        
        // Vérifier les valeurs
        assert FixTags.MSG_TYPE_NEW_ORDER.equals("D") : "MSG_TYPE_NEW_ORDER should be D";
        assert FixTags.MSG_TYPE_EXECUTION_REPORT.equals("8") : "MSG_TYPE_EXECUTION_REPORT should be 8";
        assert FixTags.SIDE_BUY.equals("1") : "SIDE_BUY should be 1";
        assert FixTags.SIDE_SELL.equals("2") : "SIDE_SELL should be 2";
        assert FixTags.ORD_STATUS_FILLED.equals("2") : "ORD_STATUS_FILLED should be 2";
        assert FixTags.ORD_STATUS_REJECTED.equals("8") : "ORD_STATUS_REJECTED should be 8";
        
        System.out.println("✓ Value constants defined correctly");
        System.out.println("✅ Test 1 PASSED\n");
    }
    
    /**
     * Test 2 : Création d'un message
     */
    private static void testFixMessageCreation() {
        System.out.println("━━━ Test 2: FixMessage Creation ━━━");
        
        FixMessage message = new FixMessage();
        assert message != null : "Message should be created";
        assert message.getAllFields() != null : "Fields map should exist";
        assert message.getAllFields().isEmpty() : "Fields should be empty initially";
        
        System.out.println("✓ Empty message created");
        System.out.println("✅ Test 2 PASSED\n");
    }
    
    /**
     * Test 3 : Getters et Setters
     */
    private static void testFixMessageGettersSetters() {
        System.out.println("━━━ Test 3: Getters and Setters ━━━");
        
        FixMessage message = new FixMessage();
        
        // Test SenderCompID
        message.setSenderCompId("123456");
        assert message.getSenderCompId().equals("123456") : "SenderCompID should be 123456";
        System.out.println("✓ SenderCompID: " + message.getSenderCompId());
        
        // Test TargetCompID
        message.setTargetCompId("789012");
        assert message.getTargetCompId().equals("789012") : "TargetCompID should be 789012";
        System.out.println("✓ TargetCompID: " + message.getTargetCompId());
        
        // Test MsgType
        message.setMsgType("D");
        assert message.getMsgType().equals("D") : "MsgType should be D";
        System.out.println("✓ MsgType: " + message.getMsgType());
        
        // Test Symbol
        message.setSymbol("AAPL");
        assert message.getSymbol().equals("AAPL") : "Symbol should be AAPL";
        System.out.println("✓ Symbol: " + message.getSymbol());
        
        // Test champs génériques
        message.setField(FixTags.SIDE, FixTags.SIDE_BUY);
        assert message.getField(FixTags.SIDE).equals("1") : "Side should be 1";
        System.out.println("✓ Side: " + message.getField(FixTags.SIDE));
        
        message.setField(FixTags.ORDER_QTY, "100");
        assert message.getField(FixTags.ORDER_QTY).equals("100") : "OrderQty should be 100";
        System.out.println("✓ OrderQty: " + message.getField(FixTags.ORDER_QTY));
        
        message.setField(FixTags.PRICE, "150.50");
        assert message.getField(FixTags.PRICE).equals("150.50") : "Price should be 150.50";
        System.out.println("✓ Price: " + message.getField(FixTags.PRICE));
        
        System.out.println("✅ Test 3 PASSED\n");
    }
    
    /**
     * Test 4 : Types de messages
     */
    private static void testFixMessageTypes() {
        System.out.println("━━━ Test 4: Message Type Detection ━━━");
        
        // Test BUY order
        FixMessage buyOrder = new FixMessage();
        buyOrder.setMsgType(FixTags.MSG_TYPE_NEW_ORDER);
        buyOrder.setField(FixTags.SIDE, FixTags.SIDE_BUY);
        
        assert buyOrder.isBuyOrder() : "Should be a BUY order";
        assert !buyOrder.isSellOrder() : "Should not be a SELL order";
        assert !buyOrder.isExecutionReport() : "Should not be an ExecutionReport";
        System.out.println("✓ BUY order detected correctly");
        
        // Test SELL order
        FixMessage sellOrder = new FixMessage();
        sellOrder.setMsgType(FixTags.MSG_TYPE_NEW_ORDER);
        sellOrder.setField(FixTags.SIDE, FixTags.SIDE_SELL);
        
        assert !sellOrder.isBuyOrder() : "Should not be a BUY order";
        assert sellOrder.isSellOrder() : "Should be a SELL order";
        assert !sellOrder.isExecutionReport() : "Should not be an ExecutionReport";
        System.out.println("✓ SELL order detected correctly");
        
        // Test ExecutionReport
        FixMessage execReport = new FixMessage();
        execReport.setMsgType(FixTags.MSG_TYPE_EXECUTION_REPORT);
        
        assert !execReport.isBuyOrder() : "Should not be a BUY order";
        assert !execReport.isSellOrder() : "Should not be a SELL order";
        assert execReport.isExecutionReport() : "Should be an ExecutionReport";
        System.out.println("✓ ExecutionReport detected correctly");
        
        System.out.println("✅ Test 4 PASSED\n");
    }
    
    /**
     * Test 5 : Calcul du checksum
     */
    private static void testChecksumCalculation() {
        System.out.println("━━━ Test 5: Checksum Calculation ━━━");
        
        FixMessage message = new FixMessage();
        message.setSenderCompId("123456");
        message.setTargetCompId("789012");
        message.setMsgType("D");
        message.setSymbol("AAPL");
        message.setField(FixTags.SIDE, "1");
        message.setField(FixTags.ORDER_QTY, "100");
        message.setField(FixTags.PRICE, "150.50");
        
        String checksum = message.calculateChecksum();
        
        assert checksum != null : "Checksum should not be null";
        assert checksum.length() == 3 : "Checksum should be 3 digits";
        assert checksum.matches("\\d{3}") : "Checksum should be numeric";
        
        System.out.println("✓ Checksum calculated: " + checksum);
        System.out.println("✓ Checksum format valid (3 digits)");
        
        // Vérifier que le checksum est déterministe
        String checksum2 = message.calculateChecksum();
        assert checksum.equals(checksum2) : "Checksum should be deterministic";
        System.out.println("✓ Checksum is deterministic");
        
        System.out.println("✅ Test 5 PASSED\n");
    }
    
    /**
     * Test 6 : Validation du checksum
     */
    private static void testChecksumValidation() {
        System.out.println("━━━ Test 6: Checksum Validation ━━━");
        
        FixMessage message = new FixMessage();
        message.setSenderCompId("123456");
        message.setTargetCompId("789012");
        message.setMsgType("D");
        
        // Calculer le bon checksum
        String correctChecksum = message.calculateChecksum();
        message.setField(FixTags.CHECKSUM, correctChecksum);
        
        assert message.isChecksumValid() : "Valid checksum should pass";
        System.out.println("✓ Valid checksum accepted: " + correctChecksum);
        
        // Tester un mauvais checksum
        message.setField(FixTags.CHECKSUM, "999");
        assert !message.isChecksumValid() : "Invalid checksum should fail";
        System.out.println("✓ Invalid checksum rejected: 999");
        
        // Tester sans checksum
        FixMessage noChecksum = new FixMessage();
        noChecksum.setSenderCompId("111111");
        assert !noChecksum.isChecksumValid() : "Missing checksum should fail";
        System.out.println("✓ Missing checksum rejected");
        
        System.out.println("✅ Test 6 PASSED\n");
    }
    
    /**
     * Test 7 : Parsing d'un message
     */
    private static void testMessageParsing() {
        System.out.println("━━━ Test 7: Message Parsing ━━━");
        
        // Message brut au format FIX
        String rawMessage = "49=123456|56=789012|35=D|55=AAPL|54=1|38=100|44=150.50|10=042|";
        
        FixMessage parsed = FixMessage.parse(rawMessage);
        
        assert parsed != null : "Parsed message should not be null";
        assert parsed.getSenderCompId().equals("123456") : "SenderCompID should be parsed";
        assert parsed.getTargetCompId().equals("789012") : "TargetCompID should be parsed";
        assert parsed.getMsgType().equals("D") : "MsgType should be parsed";
        assert parsed.getSymbol().equals("AAPL") : "Symbol should be parsed";
        assert parsed.getField(FixTags.SIDE).equals("1") : "Side should be parsed";
        assert parsed.getField(FixTags.ORDER_QTY).equals("100") : "OrderQty should be parsed";
        assert parsed.getField(FixTags.PRICE).equals("150.50") : "Price should be parsed";
        assert parsed.getField(FixTags.CHECKSUM).equals("042") : "Checksum should be parsed";
        
        System.out.println("✓ Parsed message: " + rawMessage);
        System.out.println("  - Sender: " + parsed.getSenderCompId());
        System.out.println("  - Target: " + parsed.getTargetCompId());
        System.out.println("  - Symbol: " + parsed.getSymbol());
        System.out.println("  - Side: " + parsed.getField(FixTags.SIDE));
        System.out.println("  - Quantity: " + parsed.getField(FixTags.ORDER_QTY));
        System.out.println("  - Price: " + parsed.getField(FixTags.PRICE));
        
        System.out.println("✅ Test 7 PASSED\n");
    }
    
    /**
     * Test 8 : Sérialisation d'un message
     */
    private static void testMessageSerialization() {
        System.out.println("━━━ Test 8: Message Serialization ━━━");
        
        // Créer un message
        FixMessage message = new FixMessage();
        message.setSenderCompId("111111");
        message.setTargetCompId("222222");
        message.setMsgType("D");
        message.setSymbol("GOOGL");
        message.setField(FixTags.SIDE, "2");
        message.setField(FixTags.ORDER_QTY, "50");
        message.setField(FixTags.PRICE, "2800.00");
        
        String checksum = message.calculateChecksum();
        message.setField(FixTags.CHECKSUM, checksum);
        
        // Sérialiser
        String serialized = message.toString();
        
        assert serialized != null : "Serialized message should not be null";
        assert !serialized.isEmpty() : "Serialized message should not be empty";
        assert serialized.contains("49=111111") : "Should contain SenderCompID";
        assert serialized.contains("56=222222") : "Should contain TargetCompID";
        assert serialized.contains("35=D") : "Should contain MsgType";
        assert serialized.contains("55=GOOGL") : "Should contain Symbol";
        assert serialized.contains("54=2") : "Should contain Side";
        assert serialized.contains("10=" + checksum) : "Should contain Checksum";
        
        System.out.println("✓ Serialized: " + serialized);
        
        // Test round-trip (serialize -> parse -> serialize)
        FixMessage reparsed = FixMessage.parse(serialized);
        String reserialized = reparsed.toString();
        
        // Vérifier que les champs essentiels sont identiques
        assert reparsed.getSenderCompId().equals(message.getSenderCompId()) : "Round-trip SenderCompID";
        assert reparsed.getTargetCompId().equals(message.getTargetCompId()) : "Round-trip TargetCompID";
        assert reparsed.getMsgType().equals(message.getMsgType()) : "Round-trip MsgType";
        assert reparsed.getSymbol().equals(message.getSymbol()) : "Round-trip Symbol";
        
        System.out.println("✓ Round-trip successful");
        
        System.out.println("✅ Test 8 PASSED\n");
    }
}