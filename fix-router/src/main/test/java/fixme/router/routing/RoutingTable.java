public class RoutingTable {
    
}
package fixme.router.routing;

import fixme.router.ComponentType;
import fixme.router.nio.ClientConnection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for RoutingTable - Client routing management
 */
@DisplayName("RoutingTable Tests")
class RoutingTableTest {
    
    private RoutingTable routingTable;
    private ClientConnection mockConnection1;
    private ClientConnection mockConnection2;
    private ClientConnection mockConnection3;
    
    @BeforeEach
    void setUp() {
        routingTable = new RoutingTable();
        
        // Create mock connections
        mockConnection1 = mock(ClientConnection.class);
        when(mockConnection1.getClientId()).thenReturn("B00001");
        when(mockConnection1.getType()).thenReturn(ComponentType.BROKER);
        
        mockConnection2 = mock(ClientConnection.class);
        when(mockConnection2.getClientId()).thenReturn("M00001");
        when(mockConnection2.getType()).thenReturn(ComponentType.MARKET);
        
        mockConnection3 = mock(ClientConnection.class);
        when(mockConnection3.getClientId()).thenReturn("B00002");
        when(mockConnection3.getType()).thenReturn(ComponentType.BROKER);
    }
    
    // ========================================
    // ADD ROUTE
    // ========================================
    
    @Test
    @DisplayName("Should add route successfully")
    void testAddRoute() {
        routingTable.addRoute("B00001", mockConnection1);
        
        assertEquals(1, routingTable.size());
        assertTrue(routingTable.hasRoute("B00001"));
    }
    
    @Test
    @DisplayName("Should add multiple routes")
    void testAddMultipleRoutes() {
        routingTable.addRoute("B00001", mockConnection1);
        routingTable.addRoute("M00001", mockConnection2);
        routingTable.addRoute("B00002", mockConnection3);
        
        assertEquals(3, routingTable.size());
        assertTrue(routingTable.hasRoute("B00001"));
        assertTrue(routingTable.hasRoute("M00001"));
        assertTrue(routingTable.hasRoute("B00002"));
    }
    
    @Test
    @DisplayName("Should replace existing route with same ID")
    void testReplaceRoute() {
        routingTable.addRoute("B00001", mockConnection1);
        
        ClientConnection newConnection = mock(ClientConnection.class);
        when(newConnection.getClientId()).thenReturn("B00001");
        
        routingTable.addRoute("B00001", newConnection);
        
        assertEquals(1, routingTable.size(), "Size should still be 1 (replaced)");
        assertEquals(newConnection, routingTable.findRoute("B00001"));
    }
    
    // ========================================
    // FIND ROUTE
    // ========================================
    
    @Test
    @DisplayName("Should find existing route")
    void testFindExistingRoute() {
        routingTable.addRoute("B00001", mockConnection1);
        
        ClientConnection found = routingTable.findRoute("B00001");
        
        assertNotNull(found);
        assertEquals(mockConnection1, found);
        assertEquals("B00001", found.getClientId());
    }
    
    @Test
    @DisplayName("Should return null for non-existent route")
    void testFindNonExistentRoute() {
        ClientConnection found = routingTable.findRoute("B99999");
        
        assertNull(found);
    }
    
    @Test
    @DisplayName("Should return null for null ID")
    void testFindNullId() {
        routingTable.addRoute("B00001", mockConnection1);
        
        ClientConnection found = routingTable.findRoute(null);
        
        assertNull(found);
    }
    
    @Test
    @DisplayName("Should find correct route among multiple")
    void testFindCorrectRouteAmongMultiple() {
        routingTable.addRoute("B00001", mockConnection1);
        routingTable.addRoute("M00001", mockConnection2);
        routingTable.addRoute("B00002", mockConnection3);
        
        ClientConnection found = routingTable.findRoute("M00001");
        
        assertNotNull(found);
        assertEquals(mockConnection2, found);
        assertEquals("M00001", found.getClientId());
    }
    
    // ========================================
    // REMOVE ROUTE
    // ========================================
    
    @Test
    @DisplayName("Should remove existing route")
    void testRemoveExistingRoute() {
        routingTable.addRoute("B00001", mockConnection1);
        
        assertEquals(1, routingTable.size());
        
        routingTable.removeRoute("B00001");
        
        assertEquals(0, routingTable.size());
        assertFalse(routingTable.hasRoute("B00001"));
        assertNull(routingTable.findRoute("B00001"));
    }
    
    @Test
    @DisplayName("Should handle removing non-existent route")
    void testRemoveNonExistentRoute() {
        routingTable.addRoute("B00001", mockConnection1);
        
        assertEquals(1, routingTable.size());
        
        routingTable.removeRoute("B99999");  // Doesn't exist
        
        assertEquals(1, routingTable.size(), "Size should not change");
        assertTrue(routingTable.hasRoute("B00001"), "Existing route should still be there");
    }
    
    @Test
    @DisplayName("Should remove only specified route")
    void testRemoveOnlySpecifiedRoute() {
        routingTable.addRoute("B00001", mockConnection1);
        routingTable.addRoute("M00001", mockConnection2);
        routingTable.addRoute("B00002", mockConnection3);
        
        routingTable.removeRoute("M00001");
        
        assertEquals(2, routingTable.size());
        assertFalse(routingTable.hasRoute("M00001"));
        assertTrue(routingTable.hasRoute("B00001"));
        assertTrue(routingTable.hasRoute("B00002"));
    }
    
    // ========================================
    // HAS ROUTE
    // ========================================
    
    @Test
    @DisplayName("hasRoute should return true for existing route")
    void testHasRouteTrue() {
        routingTable.addRoute("B00001", mockConnection1);
        
        assertTrue(routingTable.hasRoute("B00001"));
    }
    
    @Test
    @DisplayName("hasRoute should return false for non-existent route")
    void testHasRouteFalse() {
        assertFalse(routingTable.hasRoute("B99999"));
    }
    
    @Test
    @DisplayName("hasRoute should return false after removal")
    void testHasRouteAfterRemoval() {
        routingTable.addRoute("B00001", mockConnection1);
        assertTrue(routingTable.hasRoute("B00001"));
        
        routingTable.removeRoute("B00001");
        assertFalse(routingTable.hasRoute("B00001"));
    }
    
    // ========================================
    // SIZE
    // ========================================
    
    @Test
    @DisplayName("Empty table should have size 0")
    void testEmptyTableSize() {
        assertEquals(0, routingTable.size());
    }
    
    @Test
    @DisplayName("Size should reflect number of routes")
    void testSizeReflectsRoutes() {
        assertEquals(0, routingTable.size());
        
        routingTable.addRoute("B00001", mockConnection1);
        assertEquals(1, routingTable.size());
        
        routingTable.addRoute("M00001", mockConnection2);
        assertEquals(2, routingTable.size());
        
        routingTable.addRoute("B00002", mockConnection3);
        assertEquals(3, routingTable.size());
        
        routingTable.removeRoute("M00001");
        assertEquals(2, routingTable.size());
        
        routingTable.removeRoute("B00001");
        routingTable.removeRoute("B00002");
        assertEquals(0, routingTable.size());
    }
    
    // ========================================
    // CLEAR
    // ========================================
    
    @Test
    @DisplayName("clear should remove all routes")
    void testClear() {
        routingTable.addRoute("B00001", mockConnection1);
        routingTable.addRoute("M00001", mockConnection2);
        routingTable.addRoute("B00002", mockConnection3);
        
        assertEquals(3, routingTable.size());
        
        routingTable.clear();
        
        assertEquals(0, routingTable.size());
        assertFalse(routingTable.hasRoute("B00001"));
        assertFalse(routingTable.hasRoute("M00001"));
        assertFalse(routingTable.hasRoute("B00002"));
    }
    
    @Test
    @DisplayName("clear on empty table should not cause issues")
    void testClearEmptyTable() {
        assertEquals(0, routingTable.size());
        
        routingTable.clear();
        
        assertEquals(0, routingTable.size());
    }
    
    // ========================================
    // CONCURRENT OPERATIONS
    // ========================================
    
    @Test
    @DisplayName("Should handle concurrent additions safely")
    void testConcurrentAdditions() throws InterruptedException {
        int numThreads = 10;
        Thread[] threads = new Thread[numThreads];
        
        for (int i = 0; i < numThreads; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                ClientConnection conn = mock(ClientConnection.class);
                when(conn.getClientId()).thenReturn("B" + String.format("%05d", index));
                routingTable.addRoute("B" + String.format("%05d", index), conn);
            });
            threads[i].start();
        }
        
        for (Thread thread : threads) {
            thread.join();
        }
        
        assertEquals(numThreads, routingTable.size());
    }
    
    // ========================================
    // EDGE CASES
    // ========================================
    
    @Test
    @DisplayName("Should handle many routes")
    void testManyRoutes() {
        for (int i = 1; i <= 1000; i++) {
            ClientConnection conn = mock(ClientConnection.class);
            String id = "B" + String.format("%05d", i);
            when(conn.getClientId()).thenReturn(id);
            
            routingTable.addRoute(id, conn);
        }
        
        assertEquals(1000, routingTable.size());
        
        // Verify some random routes
        assertNotNull(routingTable.findRoute("B00001"));
        assertNotNull(routingTable.findRoute("B00500"));
        assertNotNull(routingTable.findRoute("B01000"));
    }
    
    @Test
    @DisplayName("Should handle add-remove cycles")
    void testAddRemoveCycles() {
        for (int cycle = 0; cycle < 10; cycle++) {
            routingTable.addRoute("B00001", mockConnection1);
            routingTable.addRoute("M00001", mockConnection2);
            
            assertEquals(2, routingTable.size());
            
            routingTable.removeRoute("B00001");
            routingTable.removeRoute("M00001");
            
            assertEquals(0, routingTable.size());
        }
    }
}