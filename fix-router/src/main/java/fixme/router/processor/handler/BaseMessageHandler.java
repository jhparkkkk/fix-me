package fixme.router.processor.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fixme.router.nio.ClientConnection;

/**
 * Base class for message handlers with error handling support.
 * Provides common error reporting with severity levels.
 */
public abstract class BaseMessageHandler implements MessageHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(BaseMessageHandler.class);
    
    protected void handleError(ClientConnection connection, String errorMessage, ErrorSeverity severity) {
        String clientId = connection.getClientId();
        
        switch (severity) {
            case SECURITY:
                logger.warn("SECURITY error for {}: {}", clientId, errorMessage);
                sendErrorAndClose(connection, errorMessage);
                break;
                
            case RECOVERABLE:
                boolean maxReached = connection.incrementErrorCount();
                int errorCount = connection.getErrorCount();
                
                if (maxReached) {
                    logger.warn("Max errors reached for {}: {}/5", clientId, errorCount);
                    sendErrorAndClose(connection, 
                        String.format("%s (error limit reached: %d/5)", errorMessage, errorCount));
                } else {
                    logger.warn("Recoverable error for {}: {} ({}/5)", 
                               clientId, errorMessage, errorCount);
                    sendError(connection, 
                        String.format("%s (error %d/5)", errorMessage, errorCount));
                }
                break;
                
            case ROUTING:
                logger.warn("Routing error for {}: {}", clientId, errorMessage);
                sendError(connection, errorMessage);
                break;
        }
    }
    
    private void sendError(ClientConnection connection, String errorMessage) {
        String errorMsg = String.format("ERROR|%s|", errorMessage);
        connection.queueMessage(errorMsg);
        connection.enableWriteInterest();
    }
    
    private void sendErrorAndClose(ClientConnection connection, String errorMessage) {
        String errorMsg = String.format("ERROR|%s|", errorMessage);
        
        logger.info("Closing connection {} due to: {}", 
                   connection.getClientId(), errorMessage);
        
        connection.queueMessage(errorMsg);
        connection.enableWriteInterest();
        connection.markForClosure();
    }
}