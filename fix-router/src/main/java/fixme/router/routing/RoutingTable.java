package fixme.router.routing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fixme.router.nio.ClientConnection;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Routing table that maps client IDs to their connections.
 * Allows the router to look up where to forward messages.
 */
public class RoutingTable {
    private static final Logger logger = LoggerFactory.getLogger(RoutingTable.class);

    private final ConcurrentHashMap<String, ClientConnection> routes;

    public RoutingTable() {
        this.routes = new ConcurrentHashMap<>();
    }

    public void addRoute(String clientId, ClientConnection connection) {
        routes.put(clientId, connection);
        logger.info("Added route for clientId: {}", clientId);
        logger.debug("Total routes: {}", routes.size());
    }

    public void removeRoute(String clientId) {
        ClientConnection removed = routes.remove(clientId);
        if (removed != null) {
            logger.info("Removed route for clientId: {}", clientId);
            logger.debug("Total routes: {}", routes.size());
    }
    }

    public ClientConnection findRoute(String clientId) {
        return routes.get(clientId);
    }

    public boolean hasRoute(String clientId) {
        return routes.containsKey(clientId);
    }

    public int size() {
        return routes.size();
    }

    public void clear() {
        int size = routes.size();
        routes.clear();
        logger.info("Cleared all {} routes from routing table.", size);
    }
}
