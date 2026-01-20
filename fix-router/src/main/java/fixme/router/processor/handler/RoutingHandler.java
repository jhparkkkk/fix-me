package fixme.router.processor.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fixme.common.message.FixMessage;
import fixme.common.message.FixTags;
import fixme.router.nio.ClientConnection;
import fixme.router.processor.MessageContext;
import fixme.router.routing.RoutingTable;

/**
 * Routes the message to the appropriate target based on TargetCompID.
 */
public class RoutingHandler implements MessageHandler {

    private static final Logger logger = LoggerFactory.getLogger(RoutingHandler.class);
    private final RoutingTable routingTable;

    public RoutingHandler(RoutingTable routingTable) {
        this.routingTable = routingTable;
    }

    @Override
    public boolean handle(MessageContext context) {
        FixMessage message = context.getFixMessage();

        String targetId = message.getTargetCompId();

        if (targetId == null || targetId.isEmpty()) {
            context.fail("Missing TargetCompID (Tag " + FixTags.TARGET_COMP_ID + ")");
            logger.warn("Routing failed for {}: Missing TargetCompID", context.getSource().getClientId());
            return false;
        }

        ClientConnection target = routingTable.findRoute(targetId);

        if (target == null) {
            context.fail("Destination not found: " + targetId);
            logger.warn("Routing failed for {}: Destination not found: {}", context.getSource().getClientId(), targetId);
            return false;
        }
        context.setTarget(target);
        logger.info("Routing message from {} to {}", context.getSource().getClientId(), targetId);
        return true;
    }
}