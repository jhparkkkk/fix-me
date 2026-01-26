package fixme.market;

import fixme.common.message.FixMessage;
import fixme.common.message.FixMessageFactory;
import fixme.common.message.FixTags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Market application that executes orders from brokers.
 * 
 * Responsibilities:
 * 1. Connect to router on port 5001
 * 2. Receive broker orders (NewOrderSingle)
 * 3. Check if order can be executed (inventory)
 * 4. Send execution report (Filled or Rejected)
 * 5. Update inventory on successful execution
 */
public class MarketApplication {
    
    private static final Logger logger = LoggerFactory.getLogger(MarketApplication.class);
    
    public static void main(String[] args) {
        logger.info("=".repeat(60));
        logger.info("Starting FIX Market...");
        logger.info("=".repeat(60));
        
        MarketClient client = new MarketClient();
        OrderBook orderBook = new OrderBook();
        
        try {
            client.connect();
            
            String marketId = client.getMarketId();
            logger.info("Market {} ready to execute orders", marketId);
            
            orderBook.displayInventory();
            
            System.out.println("\nWaiting for orders from brokers...\n");
            
            while (client.isConnected()) {
                String message = client.receiveMessage();
                if (message == null) {
                    logger.info("Connection closed by router");
                    break;
                }
                
                handleOrder(client, marketId, orderBook, message);
            }
            
            client.close();
            
        } catch (IOException e) {
            logger.error("Failed to start market: {}", e.getMessage());
            System.err.println("ERROR: Router not running?");
            System.exit(1);
        }
        
        logger.info("Market stopped");
    }
    
    private static void handleOrder(MarketClient client, String marketId, 
                                    OrderBook orderBook, String rawMessage) {
        if (rawMessage.startsWith("[")) {
            int idx = rawMessage.indexOf(']');
            if (idx > 0) {
                rawMessage = rawMessage.substring(idx + 1).trim();
            }
        }
        
        try {
            FixMessage order = FixMessage.parse(rawMessage);
            
            String msgType = order.getMsgType();
            
            if (FixTags.MSG_TYPE_NEW_ORDER.equals(msgType)) {
                processNewOrder(client, marketId, orderBook, order);
            } else {
                logger.warn("Unknown message type: {}", msgType);
            }
            
        } catch (Exception e) {
            logger.error("Error processing order: {}", rawMessage, e);
        }
    }
    
    private static void processNewOrder(MarketClient client, String marketId,
                                       OrderBook orderBook, FixMessage order) throws IOException {
        
        // Extract order details
        String brokerId = order.getSenderCompId();
        String symbol = order.getSymbol();
        String side = order.getField(FixTags.SIDE);
        String qtyStr = order.getField(FixTags.ORDER_QTY);
        String priceStr = order.getField(FixTags.PRICE);
        
        if (symbol == null || side == null || qtyStr == null) {
            logger.warn("Invalid order: missing required fields");
            sendRejection(client, marketId, brokerId, symbol, "Missing required fields");
            return;
        }
        
        int quantity;
        double price;
        
        try {
            quantity = Integer.parseInt(qtyStr);
            price = priceStr != null ? Double.parseDouble(priceStr) : 0.0;
        } catch (NumberFormatException e) {
            logger.warn("Invalid order: bad number format");
            sendRejection(client, marketId, brokerId, symbol, "Invalid quantity or price");
            return;
        }
        
        boolean isBuy = "1".equals(side);
        String sideStr = isBuy ? "BUY" : "SELL";
        
        logger.info("Received {} order: {} x{} @ ${} from {}", 
                   sideStr, symbol, quantity, price, brokerId);
        
        if (!orderBook.isTradedSymbol(symbol)) {
            String reason = String.format("Symbol %s not traded on this market", symbol);
            sendRejection(client, marketId, brokerId, symbol, reason);
            displayRejection(symbol, quantity, sideStr, reason);
            return;
        }
        
        if (!orderBook.canExecute(symbol, quantity, isBuy)) {
            int available = orderBook.getAvailable(symbol);
            String reason = String.format("Insufficient quantity (available: %d)", available);
            sendRejection(client, marketId, brokerId, symbol, reason);
            displayRejection(symbol, quantity, sideStr, reason);
            return;
        }
        
        orderBook.execute(symbol, quantity, isBuy);
        
        FixMessage report = FixMessageFactory.createFilledReport(
            marketId, brokerId, symbol, quantity, price
        );
        
        client.sendMessage(report.toString());
        
        displayExecution(symbol, quantity, price, sideStr, orderBook.getAvailable(symbol));
    }
    
    private static void sendRejection(MarketClient client, String marketId, 
                                     String brokerId, String symbol, String reason) throws IOException {
        FixMessage report = FixMessageFactory.createRejectedReport(
            marketId, brokerId, symbol != null ? symbol : "UNKNOWN", reason
        );
        
        client.sendMessage(report.toString());
        logger.info("Sent rejection to {}: {}", brokerId, reason);
    }
    
    private static void displayExecution(String symbol, int quantity, double price, 
                                        String side, int remaining) {
        System.out.println("\n" + "─".repeat(60));
        System.out.println("ORDER FILLED");
        System.out.printf("   %-8s %s x%,d @ $%.2f%n", side, symbol, quantity, price);
        System.out.printf("   Remaining inventory: %,d shares%n", remaining);
        System.out.println("─".repeat(60));
    }
    
    private static void displayRejection(String symbol, int quantity, String side, String reason) {
        System.out.println("\n" + "─".repeat(60));
        System.out.println("ORDER REJECTED");
        System.out.printf("   %-8s %s x%,d%n", side, symbol, quantity);
        System.out.printf("   Reason: %s%n", reason);
        System.out.println("─".repeat(60));
    }
}