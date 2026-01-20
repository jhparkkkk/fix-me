package fixme.router.connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fixme.router.nio.ClientConnection;
import fixme.router.ComponentType;
import fixme.router.IdGenerator;
import java.nio.channels.SocketChannel;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the lifecycle of client connections to the router.
 * Thread-safe registry of active connections.
*/
public class ConnectionManager {
    
    private static final Logger logger = LoggerFactory.getLogger(ConnectionManager.class);

    private final Map<String, ClientConnection> connections;
    private final IdGenerator idGenerator;


    public ConnectionManager() {
        this.connections = new ConcurrentHashMap<>();
        this.idGenerator = new IdGenerator();

    }

    public ClientConnection registerConnection(SocketChannel channel, ComponentType type) {
        String clientId = idGenerator.generateId(type);
        ClientConnection connection = new ClientConnection(clientId, channel, type);
        connections.put(clientId, connection);
        logger.info("Registered new connection: {} of type {}", clientId, type);
        return connection;
    }

    public void unregisterConnection(String clientId) {
        ClientConnection removed =connections.remove(clientId);
        if (removed != null) {
            logger.info("unregistered connection: {}", clientId);
            logger.info("Current active connections: {}", connections.size());
        }
    }

    public ClientConnection getConnection(String clientId) {
        return connections.get(clientId);
    }

    public Collection<ClientConnection> getAllConnections() {
        return connections.values();
    }

    public int getConnectionCount() {
        return connections.size();
    }

    public long getConnectionCount(ComponentType type) {
        return connections.values().stream()
                .filter(conn -> conn.getType() == type)
                .count();
    }
}
