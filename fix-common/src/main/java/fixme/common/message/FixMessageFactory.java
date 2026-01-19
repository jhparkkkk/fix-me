package fixme.common.message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fixme.common.config.FixConfig;
import fixme.common.config.FixMessageType;

/**
 * Factory class for creating FIX messages following the Factory design pattern.
 * Provides methods to create validated FIX messages of various types.
 */
public class FixMessageFactory {
    
    private static final Logger logger = LoggerFactory.getLogger(FixMessageFactory.class);
    private static final FixConfig config = FixConfig.getInstance();
    
    private FixMessageFactory() {
        throw new UnsupportedOperationException("This is a factory class and cannot be instantiated");
    }
    
    /**
     * Creates a FIX message from a raw string representation.
     * The message is parsed and validated.
     * 
     * @param rawMessage The raw FIX message string
     * @return A parsed FixMessage object
     * @throws IllegalArgumentException if the message is invalid
     */
    public static FixMessage fromString(String rawMessage) {
        if (rawMessage == null || rawMessage.trim().isEmpty()) {
            throw new IllegalArgumentException("Raw message cannot be null or empty");
        }
        
        logger.debug("Parsing raw message: {}", rawMessage);
        FixMessage message = FixMessage.parse(rawMessage);
        
        if (!message.isChecksumValid()) {
            logger.warn("Invalid checksum for message: {}", rawMessage);
            throw new IllegalArgumentException("Invalid message checksum");
        }
        
        validateMessage(message);
        
        return message;
    }
    
    /**
     * Creates a new NewOrderSingle (Buy/Sell) message.
     * 
     * @param senderCompId The sender company ID
     * @param targetCompId The target company ID (Market)
     * @param symbol The instrument symbol
     * @param side The side (Buy=1, Sell=2)
     * @param orderQty The quantity to order
     * @param price The price (optional, can be null)
     * @return A new FixMessage configured as NewOrderSingle
     */
    public static FixMessage createNewOrderSingle(
            String senderCompId, 
            String targetCompId,
            String symbol,
            String side,
            int orderQty,
            Double price) {
        
        logger.debug("Creating NewOrderSingle: sender={}, target={}, symbol={}, side={}, qty={}, price={}",
                senderCompId, targetCompId, symbol, side, orderQty, price);
        
        FixMessage message = new FixMessage();
        
        message.setSenderCompId(senderCompId);
        message.setTargetCompId(targetCompId);
        message.setMsgType(FixTags.MSG_TYPE_NEW_ORDER);

        message.setSymbol(symbol);
        message.setField(FixTags.SIDE, side);
        message.setField(FixTags.ORDER_QTY, String.valueOf(orderQty));
        
        if (price != null) {
            message.setField(FixTags.PRICE, String.format("%.2f", price));
        }
        
        // Calculate and set checksum
        String checksum = message.calculateChecksum();
        message.setField(FixTags.CHECKSUM, checksum);
        
        validateMessage(message);
        
        logger.info("Created NewOrderSingle message: {}", message);
        return message;
    }
    
    /**
     * Creates a Buy order message (convenience method).
     */
    public static FixMessage createBuyOrder(
            String senderCompId,
            String targetCompId,
            String symbol,
            int orderQty,
            Double price) {
        
        return createNewOrderSingle(
                senderCompId,
                targetCompId,
                symbol,
                FixTags.SIDE_BUY,
                orderQty,
                price
        );
    }
    
    /**
     * Creates a Sell order message (convenience method).
     */
    public static FixMessage createSellOrder(
            String senderCompId,
            String targetCompId,
            String symbol,
            int orderQty,
            Double price) {
        
        return createNewOrderSingle(
                senderCompId,
                targetCompId,
                symbol,
                FixTags.SIDE_SELL,
                orderQty,
                price
        );
    }
    
    /**
     * Creates an ExecutionReport message.
     * 
     * @param senderCompId The sender company ID (Market)
     * @param targetCompId The target company ID (Broker)
     * @param symbol The instrument symbol
     * @param ordStatus The order status (Filled=2, Rejected=8)
     * @param orderQty The quantity (optional)
     * @param price The execution price (optional)
     * @param text Rejection reason or other text (optional)
     * @return A new FixMessage configured as ExecutionReport
     */
    public static FixMessage createExecutionReport(
            String senderCompId,
            String targetCompId,
            String symbol,
            String ordStatus,
            Integer orderQty,
            Double price,
            String text) {
        
        logger.debug("Creating ExecutionReport: sender={}, target={}, symbol={}, status={}, qty={}, price={}, text={}",
                senderCompId, targetCompId, symbol, ordStatus, orderQty, price, text);
        
        FixMessage message = new FixMessage();
        
        message.setSenderCompId(senderCompId);
        message.setTargetCompId(targetCompId);
        message.setMsgType(FixTags.MSG_TYPE_EXECUTION_REPORT);
        
        message.setSymbol(symbol);
        message.setField(FixTags.ORD_STATUS, ordStatus);
        
        if (orderQty != null) {
            message.setField(FixTags.ORDER_QTY, String.valueOf(orderQty));
        }
        
        if (price != null) {
            message.setField(FixTags.PRICE, String.format("%.2f", price));
        }
        
        if (text != null && !text.trim().isEmpty()) {
            message.setField(FixTags.TEXT, text);
        }
        
        String checksum = message.calculateChecksum();
        message.setField(FixTags.CHECKSUM, checksum);
        
        validateMessage(message);
        
        logger.info("Created ExecutionReport message: {}", message);
        return message;
    }
    
    /**
     * Creates a Filled execution report (convenience method).
     */
    public static FixMessage createFilledReport(
            String senderCompId,
            String targetCompId,
            String symbol,
            int orderQty,
            double price) {
        
        return createExecutionReport(
                senderCompId,
                targetCompId,
                symbol,
                FixTags.ORD_STATUS_FILLED,
                orderQty,
                price,
                null
        );
    }
    
    /**
     * Creates a Rejected execution report (convenience method).
     */
    public static FixMessage createRejectedReport(
            String senderCompId,
            String targetCompId,
            String symbol,
            String rejectionReason) {
        
        return createExecutionReport(
                senderCompId,
                targetCompId,
                symbol,
                FixTags.ORD_STATUS_REJECTED,
                null,
                null,
                rejectionReason
        );
    }
    
    /**
     * Validates a FIX message against the configuration.
     * Checks for required tags based on message type.
     * 
     * @param message The message to validate
     * @throws IllegalArgumentException if validation fails
     */
    private static void validateMessage(FixMessage message) {
        String msgType = message.getMsgType();
        
        if (msgType == null) {
            throw new IllegalArgumentException("Message type (tag 35) is required");
        }
        
        FixMessageType messageTypeDef = config.getMessageType(msgType);
        
        if (messageTypeDef == null) {
            throw new IllegalArgumentException("Unknown message type: " + msgType);
        }
        
        for (String requiredTag : messageTypeDef.getRequiredTags()) {
            if (message.getField(requiredTag) == null) {
                String tagName = config.getTagDefinition(requiredTag) != null 
                    ? config.getTagDefinition(requiredTag).getName() 
                    : requiredTag;
                throw new IllegalArgumentException(
                    String.format("Required tag %s (%s) is missing for message type %s",
                        requiredTag, tagName, messageTypeDef.getName())
                );
            }
        }
        
        logger.debug("Message validation successful for type: {}", messageTypeDef.getName());
    }
}