package fixme.router.processor.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fixme.common.message.FixMessage;
import fixme.common.message.FixMessageFactory;
import fixme.router.processor.MessageContext;


public class ValidationHandler implements MessageHandler{

    private static final Logger logger = LoggerFactory.getLogger(ValidationHandler.class);

    @Override
    public boolean handle(MessageContext context) {
        logger.debug("Validating message from {}", context.getSource().getClientId());
        try {
            FixMessage message = FixMessageFactory.fromString(context.getRawMessage());
            context.setFixMessage(message);
            logger.info("Message validated passed for {}", context.getSource().getClientId());
            return true;
        } catch (IllegalArgumentException e) {
            String error = "Invalid FIX message format: " + e.getMessage();
            context.fail(error);
            logger.warn("Message validation failed for {}: {}", context.getSource().getClientId(), e.getMessage());
            return false;
        }
    }
    
}
