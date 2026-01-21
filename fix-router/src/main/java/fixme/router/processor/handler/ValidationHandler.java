package fixme.router.processor.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fixme.common.message.FixMessage;
import fixme.common.message.FixMessageFactory;
import fixme.common.validation.MessageValidator;
import fixme.common.validation.ValidationResult;
import fixme.router.nio.ClientConnection;
import fixme.router.processor.MessageContext;


public class ValidationHandler implements MessageHandler{

    private static final Logger logger = LoggerFactory.getLogger(ValidationHandler.class);

    @Override
    public boolean handle(MessageContext context) {
        String rawMessage = context.getRawMessage();
        String clientId = context.getSource().getClientId();

        logger.debug("Validating message from {}", clientId);
        
        // Format validation
        ValidationResult formatValidation = MessageValidator.validate(rawMessage);
        if (!formatValidation.isValid()) {
            context.fail("Format validation failed: " + formatValidation.getErrorMessage());
            sendErrorAndClose(context.getSource(), formatValidation.getErrorMessage());
            logger.warn("Message validation failed for {}: {}", clientId, formatValidation.getErrorMessage());
            return false;
        }
        logger.debug("Format validation passed for {}", clientId);
        
        // FIX message validation
        try {
            FixMessage message = FixMessageFactory.fromString(rawMessage);
            context.setFixMessage(message);
            logger.info("Message validated passed for {}", context.getSource().getClientId());
            return true;
        } catch (IllegalArgumentException e) {
            String error = "Invalid FIX message format: " + e.getMessage();
            context.fail(error);
            logger.warn("Message validation failed for {}: {}", context.getSource().getClientId(), e.getMessage());
            sendErrorAndClose(context.getSource(), error);
            return false;
        } catch (Exception e) {
            String error = "Unexpected validation error: " + e.getMessage();
            context.fail(error);
            logger.error("Unexpected validation error for {}", clientId, e);
            sendErrorAndClose(context.getSource(), error);
            return false;
        }
    }

    private void sendErrorAndClose(ClientConnection connection, String errorMessage) {
        String errorMsg = String.format("ERROR|%s|", errorMessage);
        connection.queueMessage(errorMsg);
        connection.enableWriteInterest();
        connection.markForClosure();  // ← Mark for closure
}
    
}
