package fixme.router.nio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fixme.router.processor.MessageProcessor;

import java.util.regex.Pattern;

/**
 * Dispatches received messages for processing.
 * Handles message boundary detection (delimiter-based).
 * TODO: Integrate with MessageProcessor for actual message handling.
 */
public class MessageDispatcher {
    private final MessageProcessor messageProcessor;
    private static final Logger logger = LoggerFactory.getLogger(MessageDispatcher.class);
    private final String delimiter;
    
    public MessageDispatcher(String delimiter, MessageProcessor messageProcessor) {
        this.delimiter = delimiter;
        this.messageProcessor = messageProcessor;
    }
    
    public void dispatch(String data, ClientConnection source) {
        if (data == null || data.isEmpty()) {
            return;
        }
        String cleanedData = data.replaceAll("[\\r\\n]+", "");
        
        if (!cleanedData.isEmpty()) {
            processMessage(cleanedData, source);
        }

    }
    
    private void processMessage(String message, ClientConnection source) {
        logger.info("Received message from {}: {}", source.getClientId(), message);
        
        messageProcessor.processMessage(message, source);
    }
}