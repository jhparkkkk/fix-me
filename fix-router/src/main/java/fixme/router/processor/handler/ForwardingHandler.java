package fixme.router.processor.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fixme.router.nio.ClientConnection;
import fixme.router.processor.MessageContext;

/**
 * Forwards the message to the target client connection.
 * 
 * Steps:
 * 1. Retrieve the target connection from the context.
 * 2. Queue the raw FIX message for sending on the target connection.
 * 3. The NIO server will handle the actual sending asynchronously.
 */
public class ForwardingHandler implements MessageHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(ForwardingHandler.class);

    @Override
    public boolean handle(MessageContext context) {
        ClientConnection target = context.getTarget();
        String rawMessage = context.getRawMessage();
        String senderId = context.getSource().getClientId();
    
        String formatted = String.format("[%s → %s] %s", 
            senderId, target.getClientId(), rawMessage);

        target.queueMessage(formatted);

        target.enableWriteInterest();
        
        logger.info("Forwarded message from {} to {} ({} bytes)", 
                    context.getSource().getClientId(), 
                    target.getClientId(), 
                    rawMessage.length());

        return true;
    }
    
}
