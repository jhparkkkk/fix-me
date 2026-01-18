package fixme.common.config;

/**
 * Test complet de la configuration FIX
 */
public class FixConfigTest {
    
    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════════╗");
        System.out.println("║    FIX CONFIGURATION TEST SUITE           ║");
        System.out.println("╚════════════════════════════════════════════╝\n");
        
        try {
            testLoadConfiguration();
            testTagDefinitions();
            testMessageTypes();
            testValidation();
            testTagLookup();

            printFullConfiguration();
            
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
     * Test 1 : Chargement de la configuration
     */
    private static void testLoadConfiguration() {
        System.out.println("━━━ Test 1: Load Configuration ━━━");
        
        FixConfig config = FixConfig.getInstance();
        
        // Vérifier que l'instance est créée
        assert config != null : "Config instance should not be null";
        System.out.println("✓ Config instance created");
        
        // Vérifier les métadonnées
        assert config.getProtocol() != null : "Protocol should not be null";
        assert config.getVersion() != null : "Version should not be null";
        assert config.getDescription() != null : "Definition should not be null";
        assert config.getDelimiter() != null : "Delimiter should not be null";
        
        System.out.println("✓ Protocol: " + config.getProtocol());
        System.out.println("✓ Version: " + config.getVersion());
        System.out.println("✓ Definition: " + config.getDescription());
        System.out.println("✓ Delimiter: '" + config.getDelimiter() + "'");
        
        // Vérifier le Singleton
        FixConfig config2 = FixConfig.getInstance();
        assert config == config2 : "Singleton should return same instance";
        System.out.println("✓ Singleton pattern working");
        
        System.out.println("✅ Test 1 PASSED\n");
    }
    
    /**
     * Test 2 : Définitions des tags
     */
    private static void testTagDefinitions() {
        System.out.println("━━━ Test 2: Tag Definitions ━━━");
        
        FixConfig config = FixConfig.getInstance();
        
        // Vérifier qu'on a chargé des tags
        var allTags = config.getAllTagDefinitions();
        assert !allTags.isEmpty() : "Should have loaded tags";
        System.out.println("✓ Loaded " + allTags.size() + " tags");
        
        // Test des tags système
        testTag(config, "49", "SenderCompID", true);
        testTag(config, "56", "TargetCompID", true);
        testTag(config, "35", "MsgType", true);
        testTag(config, "10", "CheckSum", true);
        
        // Test des tags d'ordre
        testTag(config, "55", "Symbol", true);
        testTag(config, "54", "Side", true);
        testTag(config, "38", "OrderQty", true);
        testTag(config, "44", "Price", false);
        
        // Test des tags d'exécution
        testTag(config, "39", "OrdStatus", true);
        testTag(config, "58", "Text", false);
        
        System.out.println("✅ Test 2 PASSED\n");
    }
    
    private static void testTag(FixConfig config, String tag, String expectedName, boolean expectedRequired) {
        FixTagDefinition def = config.getTagDefinition(tag);
        
        assert def != null : "Tag " + tag + " should exist";
        assert def.getName().equals(expectedName) : "Tag " + tag + " name should be " + expectedName;
        assert def.isRequired() == expectedRequired : "Tag " + tag + " required should be " + expectedRequired;
        
        System.out.println("  ✓ Tag " + tag + " (" + def.getName() + ") - " + 
            (def.isRequired() ? "required" : "optional"));
    }
    
    /**
     * Test 3 : Types de messages
     */
    private static void testMessageTypes() {
        System.out.println("━━━ Test 3: Message Types ━━━");
        
        FixConfig config = FixConfig.getInstance();
        
        // Test NewOrderSingle (D)
        FixMessageType newOrder = config.getMessageType("D");
        assert newOrder != null : "NewOrderSingle should exist";
        assert newOrder.getName().equals("NewOrderSingle") : "Name should be NewOrderSingle";
        assert newOrder.getRequiredTags() != null : "Required tags should not be null";
        assert newOrder.getRequiredTags().size() >= 7 : "Should have at least 7 required tags";
        
        System.out.println("✓ NewOrderSingle (D):");
        System.out.println("  - Name: " + newOrder.getName());
        System.out.println("  - Description: " + newOrder.getDescription());
        System.out.println("  - Required tags: " + newOrder.getRequiredTags());
        System.out.println("  - Optional tags: " + newOrder.getOptionalTags());
        
        // Test ExecutionReport (8)
        FixMessageType execReport = config.getMessageType("8");
        assert execReport != null : "ExecutionReport should exist";
        assert execReport.getName().equals("ExecutionReport") : "Name should be ExecutionReport";
        
        System.out.println("✓ ExecutionReport (8):");
        System.out.println("  - Name: " + execReport.getName());
        System.out.println("  - Description: " + execReport.getDescription());
        System.out.println("  - Required tags: " + execReport.getRequiredTags());
        System.out.println("  - Optional tags: " + execReport.getOptionalTags());
        
        System.out.println("✅ Test 3 PASSED\n");
    }
    
    /**
     * Test 4 : Validation des valeurs
     */
    private static void testValidation() {
        System.out.println("━━━ Test 4: Value Validation ━━━");
        
        FixConfig config = FixConfig.getInstance();
        
        // Test validation du tag Side (54)
        FixTagDefinition sideDef = config.getTagDefinition("54");
        assert sideDef != null : "Side tag should exist";
        
        // Valeurs valides
        assert sideDef.isValidValue("1") : "Side=1 (Buy) should be valid";
        assert sideDef.isValidValue("2") : "Side=2 (Sell) should be valid";
        System.out.println("✓ Side tag accepts valid values (1, 2)");
        
        // Valeur invalide
        assert !sideDef.isValidValue("999") : "Side=999 should be invalid";
        System.out.println("✓ Side tag rejects invalid value (999)");
        
        // Test validation du tag MsgType (35)
        FixTagDefinition msgTypeDef = config.getTagDefinition("35");
        assert msgTypeDef != null : "MsgType tag should exist";
        
        assert msgTypeDef.isValidValue("D") : "MsgType=D should be valid";
        assert msgTypeDef.isValidValue("8") : "MsgType=8 should be valid";
        System.out.println("✓ MsgType tag accepts valid values (D, 8)");
        
        assert !msgTypeDef.isValidValue("Z") : "MsgType=Z should be invalid";
        System.out.println("✓ MsgType tag rejects invalid value (Z)");
        
        // Test validation du tag OrdStatus (39)
        FixTagDefinition statusDef = config.getTagDefinition("39");
        assert statusDef != null : "OrdStatus tag should exist";
        
        assert statusDef.isValidValue("2") : "OrdStatus=2 (Filled) should be valid";
        assert statusDef.isValidValue("8") : "OrdStatus=8 (Rejected) should be valid";
        System.out.println("✓ OrdStatus tag accepts valid values (2, 8)");
        
        System.out.println("✅ Test 4 PASSED\n");
    }
    
    /**
     * Test 5 : Recherche de tags
     */
    private static void testTagLookup() {
        System.out.println("━━━ Test 5: Tag Lookup ━━━");
        
        FixConfig config = FixConfig.getInstance();
        
        // Recherche par numéro
        FixTagDefinition senderDef = config.getTagDefinition("49");
        assert senderDef != null : "Should find tag by number";
        assert senderDef.getName().equals("SenderCompID") : "Should have correct name";
        System.out.println("✓ Find by number: 49 → " + senderDef.getName());
        
        // Recherche par nom
        String tagNum = config.getTagNumber("SenderCompID");
        assert tagNum != null : "Should find tag number by name";
        assert tagNum.equals("49") : "Should return correct number";
        System.out.println("✓ Find by name: SenderCompID → " + tagNum);
        
        // Test d'autres tags
        testLookup(config, "56", "TargetCompID");
        testLookup(config, "55", "Symbol");
        testLookup(config, "54", "Side");
        testLookup(config, "35", "MsgType");
        
        System.out.println("✅ Test 5 PASSED\n");
    }
    
    private static void testLookup(FixConfig config, String expectedNum, String name) {
        String num = config.getTagNumber(name);
        assert num != null : name + " should have a number";
        assert num.equals(expectedNum) : name + " should be " + expectedNum;
        System.out.println("  ✓ " + name + " → " + num);
    }
    
    /**
     * Test bonus : Afficher la configuration complète
     */
    private static void printFullConfiguration() {
        System.out.println("\n━━━ Full Configuration ━━━");
        FixConfig config = FixConfig.getInstance();
        config.printConfigurationSummary();
    }
}