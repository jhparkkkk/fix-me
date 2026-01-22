package fixme.router.processor.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fixme.common.message.FixMessage;
import fixme.common.message.FixTags;
import fixme.router.nio.ClientConnection;
import fixme.router.processor.MessageContext;
import fixme.router.routing.RoutingTable;

/**
 * Routes messages to appropriate targets.
 * 
 * Error Severity: ROUTING (no disconnect, no counter)
 * 
 * Design Pattern: Chain of Responsibility + Router
 */
public class RoutingHandler extends BaseMessageHandler {

    private static final Logger logger = LoggerFactory.getLogger(RoutingHandler.class);
    private final RoutingTable routingTable;

    public RoutingHandler(RoutingTable routingTable) {
        this.routingTable = routingTable;
    }

    @Override
    public boolean handle(MessageContext context) {
        FixMessage message = context.getFixMessage();
        ClientConnection source = context.getSource();
        String sourceId = source.getClientId();

        // check si sender ID matches assigned ID
        String senderId = message.getSenderCompId();
        if (!sourceId.equals(senderId)) {
            String error = String.format("SenderCompID (%s) does not match connection ID (%s)", 
                                         senderId, sourceId);
            context.fail(error);
            handleError(source, error, ErrorSeverity.SECURITY);
            return false;
        }

        // Get target from message
        String targetId = message.getTargetCompId();

        if (targetId == null || targetId.isEmpty()) {
            String error = "Missing TargetCompID (tag 56)";
            context.fail(error);
            
            // ROUTING: Missing target, user can retry with correct target
            handleError(source, error, ErrorSeverity.ROUTING);
            return false;
        }

        // Lookup target in routing table
        ClientConnection target = routingTable.findRoute(targetId);

        if (target == null) {
            String error = String.format("Destination not found: %s", targetId);
            context.fail(error);
            
            // ROUTING: Target not connected yet, user can retry later
            handleError(source, error, ErrorSeverity.ROUTING);
            return false;
        }

        // Check if target is the same as source (loop prevention)
        if (targetId.equals(sourceId)) {
            String error = "Cannot send message to self";
            context.fail(error);
            
            // ROUTING: User mistake, can retry with correct target
            handleError(source, error, ErrorSeverity.ROUTING);
            return false;
        }

        // Success: set target in context
        context.setTarget(target);
        logger.info("Routing message from {} to {}", sourceId, targetId);
        
        return true;
    }
}