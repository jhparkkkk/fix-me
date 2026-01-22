package fixme.router.processor.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fixme.common.message.FixMessage;
import fixme.common.message.FixMessageFactory;
import fixme.common.validation.MessageValidator;
import fixme.common.validation.ValidationResult;
import fixme.router.nio.ClientConnection;
import fixme.router.processor.MessageContext;

/**
 * Validates FIX messages before routing.
 * Uses error severity levels for better UX.
 * 
 * Validation stages:
 * 1. Format validation - RECOVERABLE (counted)
 * 2. Business validation - RECOVERABLE (counted)
 * 
 * Design Pattern: Chain of Responsibility + Fail-Fast
 */
public class ValidationHandler extends BaseMessageHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(ValidationHandler.class);
    
    @Override
    public boolean handle(MessageContext context) {
        String rawMessage = context.getRawMessage();
        ClientConnection source = context.getSource();
        String clientId = source.getClientId();
        
        logger.debug("Validating message from {}", clientId);
        
        // Preliminary check: Ensure it's a single complete message
        ValidationResult singleMessageCheck = MessageValidator.validateSingleMessage(rawMessage);

        if (!singleMessageCheck.isValid()) {
            context.fail(singleMessageCheck.getErrorMessage());
            handleError(source, singleMessageCheck.getErrorMessage(), ErrorSeverity.RECOVERABLE);
            return false;
        }

        // Format Validation
        ValidationResult formatValidation = MessageValidator.validate(rawMessage);
        
        if (!formatValidation.isValid()) {
            String error = "Format validation failed: " + formatValidation.getErrorMessage();
            context.fail(error);
            
            // RECOVERABLE: Bad format, but user can fix it
            handleError(source, formatValidation.getErrorMessage(), ErrorSeverity.RECOVERABLE);
            return false;
        }
        
        logger.debug("Format validation passed for {}", clientId);
        
        // Business Validation (FIX Protocol)
        try {
            FixMessage message = FixMessageFactory.fromString(rawMessage);
            context.setFixMessage(message);
            
            logger.info("Message validation passed for {}", clientId);
            return true;
            
        } catch (IllegalArgumentException e) {
            String error = "FIX validation failed: " + e.getMessage();
            context.fail(error);
            
            // RECOVERABLE: Bad FIX message, but user can fix it
            handleError(source, e.getMessage(), ErrorSeverity.RECOVERABLE);
            return false;
        }
    }
}