package fixme.router.connection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fixme.router.ComponentType;
import fixme.router.IdGenerator;
import fixme.router.nio.ClientConnection;
import fixme.router.routing.RoutingTable;

import java.nio.channels.SocketChannel;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages all client connections and integrates with routing table.
 * 
 * Responsibilities:
 * - Register new connections (assign IDs)
 * - Maintain connection registry
 * - Update routing table
 * - Unregister connections on disconnect
 * 
 * Thread-safe: Uses ConcurrentHashMap
 * 
 * Design Pattern: Registry + Facade
 */
public class ConnectionManager {
    
    private static final Logger logger = LoggerFactory.getLogger(ConnectionManager.class);
    
    private final ConcurrentHashMap<String, ClientConnection> connections;
    private final IdGenerator idGenerator;
    private final RoutingTable routingTable;
    
    public ConnectionManager(RoutingTable routingTable) {
        this.connections = new ConcurrentHashMap<>();
        this.idGenerator = new IdGenerator();
        this.routingTable = routingTable;
    }
    
    /**
     * Register a new connection.
     * 
     * @param channel The socket channel
     * @param type The component type (BROKER or MARKET)
     * @return The created ClientConnection
     */
    public ClientConnection registerConnection(SocketChannel channel, ComponentType type) {
        // Generate unique ID
        String clientId = idGenerator.generateId(type);
        
        // Create connection
        ClientConnection connection = new ClientConnection(clientId, channel, type);
        
        // Add to registry
        connections.put(clientId, connection);
        
        // Add to routing table
        routingTable.addRoute(clientId, connection);
        
        logger.info("Registered new connection: {} of type {}", clientId, type);
        logger.debug("Total active connections: {}", connections.size());
        
        return connection;
    }
    
    /**
     * Unregister a connection (on disconnect).
     * 
     * @param clientId The client ID to remove
     */
    public void unregisterConnection(String clientId) {
        ClientConnection removed = connections.remove(clientId);
        
        if (removed != null) {
            // Remove from routing table
            routingTable.removeRoute(clientId);
            
            logger.info("Unregistered connection: {}", clientId);
            logger.debug("Total active connections: {}", connections.size());
        }
    }
    
    /**
     * Get a connection by ID.
     * 
     * @param clientId The client ID
     * @return The connection, or null if not found
     */
    public ClientConnection getConnection(String clientId) {
        return connections.get(clientId);
    }
    
    /**
     * Get total number of active connections.
     * 
     * @return Number of connections
     */
    public int getConnectionCount() {
        return connections.size();
    }
}