package fixme.router;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for IdGenerator - Unique ID generation for clients
 */
@DisplayName("IdGenerator Tests")
class IdGeneratorTest {
    
    private IdGenerator idGenerator;
    
    @BeforeEach
    void setUp() {
        idGenerator = new IdGenerator();
    }
    
    // ========================================
    // BROKER ID GENERATION
    // ========================================
    
    @Test
    @DisplayName("First broker ID should be B00001")
    void testFirstBrokerId() {
        String id = idGenerator.generateId(ComponentType.BROKER);
        
        assertEquals("B00001", id);
    }
    
    @Test
    @DisplayName("Broker IDs should increment sequentially")
    void testBrokerIdSequence() {
        String id1 = idGenerator.generateId(ComponentType.BROKER);
        String id2 = idGenerator.generateId(ComponentType.BROKER);
        String id3 = idGenerator.generateId(ComponentType.BROKER);
        
        assertEquals("B00001", id1);
        assertEquals("B00002", id2);
        assertEquals("B00003", id3);
    }
    
    @Test
    @DisplayName("Broker IDs should be exactly 6 characters")
    void testBrokerIdLength() {
        String id = idGenerator.generateId(ComponentType.BROKER);
        
        assertEquals(6, id.length(), "ID should be 6 characters (B + 5 digits)");
    }
    
    @Test
    @DisplayName("Broker ID should start with 'B'")
    void testBrokerIdPrefix() {
        String id = idGenerator.generateId(ComponentType.BROKER);
        
        assertTrue(id.startsWith("B"), "Broker ID should start with 'B'");
    }
    
    // ========================================
    // MARKET ID GENERATION
    // ========================================
    
    @Test
    @DisplayName("First market ID should be M00001")
    void testFirstMarketId() {
        String id = idGenerator.generateId(ComponentType.MARKET);
        
        assertEquals("M00001", id);
    }
    
    @Test
    @DisplayName("Market IDs should increment sequentially")
    void testMarketIdSequence() {
        String id1 = idGenerator.generateId(ComponentType.MARKET);
        String id2 = idGenerator.generateId(ComponentType.MARKET);
        String id3 = idGenerator.generateId(ComponentType.MARKET);
        
        assertEquals("M00001", id1);
        assertEquals("M00002", id2);
        assertEquals("M00003", id3);
    }
    
    @Test
    @DisplayName("Market IDs should be exactly 6 characters")
    void testMarketIdLength() {
        String id = idGenerator.generateId(ComponentType.MARKET);
        
        assertEquals(6, id.length(), "ID should be 6 characters (M + 5 digits)");
    }
    
    @Test
    @DisplayName("Market ID should start with 'M'")
    void testMarketIdPrefix() {
        String id = idGenerator.generateId(ComponentType.MARKET);
        
        assertTrue(id.startsWith("M"), "Market ID should start with 'M'");
    }
    
    // ========================================
    // INDEPENDENCE
    // ========================================
    
    @Test
    @DisplayName("Broker and Market counters should be independent")
    void testIndependentCounters() {
        // Generate some broker IDs
        String b1 = idGenerator.generateId(ComponentType.BROKER);
        String b2 = idGenerator.generateId(ComponentType.BROKER);
        
        // Generate some market IDs
        String m1 = idGenerator.generateId(ComponentType.MARKET);
        String m2 = idGenerator.generateId(ComponentType.MARKET);
        
        // Generate more broker IDs
        String b3 = idGenerator.generateId(ComponentType.BROKER);
        
        assertEquals("B00001", b1);
        assertEquals("B00002", b2);
        assertEquals("M00001", m1);
        assertEquals("M00002", m2);
        assertEquals("B00003", b3, "Broker counter should be independent of Market counter");
    }
    
    // ========================================
    // UNIQUENESS
    // ========================================
    
    @Test
    @DisplayName("100 broker IDs should all be unique")
    void testBrokerIdUniqueness() {
        Set<String> ids = new HashSet<>();
        
        for (int i = 0; i < 100; i++) {
            String id = idGenerator.generateId(ComponentType.BROKER);
            assertTrue(ids.add(id), "ID should be unique: " + id);
        }
        
        assertEquals(100, ids.size(), "Should have 100 unique IDs");
    }
    
    @Test
    @DisplayName("100 market IDs should all be unique")
    void testMarketIdUniqueness() {
        Set<String> ids = new HashSet<>();
        
        for (int i = 0; i < 100; i++) {
            String id = idGenerator.generateId(ComponentType.MARKET);
            assertTrue(ids.add(id), "ID should be unique: " + id);
        }
        
        assertEquals(100, ids.size(), "Should have 100 unique IDs");
    }
    
    @Test
    @DisplayName("Broker and Market IDs should never collide")
    void testNoCollisionBetweenTypes() {
        Set<String> allIds = new HashSet<>();
        
        // Generate 50 of each type
        for (int i = 0; i < 50; i++) {
            String brokerId = idGenerator.generateId(ComponentType.BROKER);
            String marketId = idGenerator.generateId(ComponentType.MARKET);
            
            assertTrue(allIds.add(brokerId), "Broker ID should be unique: " + brokerId);
            assertTrue(allIds.add(marketId), "Market ID should be unique: " + marketId);
        }
        
        assertEquals(100, allIds.size(), "Should have 100 unique IDs total");
    }
    
    // ========================================
    // FORMAT VALIDATION
    // ========================================
    
    @Test
    @DisplayName("All broker IDs should match format B#####")
    void testBrokerIdFormat() {
        for (int i = 0; i < 20; i++) {
            String id = idGenerator.generateId(ComponentType.BROKER);
            
            assertTrue(id.matches("B\\d{5}"), 
                      "ID should match format B##### : " + id);
        }
    }
    
    @Test
    @DisplayName("All market IDs should match format M#####")
    void testMarketIdFormat() {
        for (int i = 0; i < 20; i++) {
            String id = idGenerator.generateId(ComponentType.MARKET);
            
            assertTrue(id.matches("M\\d{5}"), 
                      "ID should match format M##### : " + id);
        }
    }
    
    // ========================================
    // COUNTER MANAGEMENT
    // ========================================
    
    @Test
    @DisplayName("getCurrentCount should return correct broker count")
    void testGetCurrentCountBroker() {
        assertEquals(0, idGenerator.getCurrentCount(ComponentType.BROKER));
        
        idGenerator.generateId(ComponentType.BROKER);
        assertEquals(1, idGenerator.getCurrentCount(ComponentType.BROKER));
        
        idGenerator.generateId(ComponentType.BROKER);
        idGenerator.generateId(ComponentType.BROKER);
        assertEquals(3, idGenerator.getCurrentCount(ComponentType.BROKER));
    }
    
    @Test
    @DisplayName("getCurrentCount should return correct market count")
    void testGetCurrentCountMarket() {
        assertEquals(0, idGenerator.getCurrentCount(ComponentType.MARKET));
        
        idGenerator.generateId(ComponentType.MARKET);
        assertEquals(1, idGenerator.getCurrentCount(ComponentType.MARKET));
        
        idGenerator.generateId(ComponentType.MARKET);
        assertEquals(2, idGenerator.getCurrentCount(ComponentType.MARKET));
    }
    
    @Test
    @DisplayName("reset should reset broker counter")
    void testResetBroker() {
        idGenerator.generateId(ComponentType.BROKER);
        idGenerator.generateId(ComponentType.BROKER);
        idGenerator.generateId(ComponentType.BROKER);
        
        assertEquals(3, idGenerator.getCurrentCount(ComponentType.BROKER));
        
        idGenerator.reset(ComponentType.BROKER);
        
        assertEquals(0, idGenerator.getCurrentCount(ComponentType.BROKER));
        
        String nextId = idGenerator.generateId(ComponentType.BROKER);
        assertEquals("B00001", nextId, "Should start from B00001 after reset");
    }
    
    @Test
    @DisplayName("reset should only affect specified type")
    void testResetIndependence() {
        idGenerator.generateId(ComponentType.BROKER);
        idGenerator.generateId(ComponentType.BROKER);
        idGenerator.generateId(ComponentType.MARKET);
        idGenerator.generateId(ComponentType.MARKET);
        
        idGenerator.reset(ComponentType.BROKER);
        
        assertEquals(0, idGenerator.getCurrentCount(ComponentType.BROKER));
        assertEquals(2, idGenerator.getCurrentCount(ComponentType.MARKET), 
                    "Market counter should not be affected");
    }
    
    // ========================================
    // EDGE CASES
    // ========================================
    
    @Test
    @DisplayName("Should handle generating many IDs")
    void testManyIds() {
        for (int i = 0; i < 1000; i++) {
            String id = idGenerator.generateId(ComponentType.BROKER);
            assertNotNull(id);
            assertEquals(6, id.length());
        }
        
        assertEquals(1000, idGenerator.getCurrentCount(ComponentType.BROKER));
    }
    
    @Test
    @DisplayName("Should throw exception when max ID reached")
    void testMaxIdReached() {
        // Generate 99999 IDs (max)
        for (int i = 0; i < 99999; i++) {
            idGenerator.generateId(ComponentType.BROKER);
        }
        
        // Next one should throw
        assertThrows(IllegalStateException.class, () -> {
            idGenerator.generateId(ComponentType.BROKER);
        }, "Should throw when max ID (99999) is reached");
    }
}