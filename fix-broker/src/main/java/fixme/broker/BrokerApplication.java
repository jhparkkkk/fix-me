package fixme.broker;

import fixme.common.message.FixMessage;
import fixme.common.message.FixMessageFactory;
import fixme.common.message.FixTags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Scanner;
import java.util.regex.Pattern;

public class BrokerApplication {
    
    private static final Logger logger = LoggerFactory.getLogger(BrokerApplication.class);
    
    // Static broker ID for prompt redisplay
    private static volatile String currentBrokerId = null;
    
    // Validation patterns
    private static final Pattern SYMBOL_PATTERN = Pattern.compile("^[A-Z]{1,10}$");
    private static final Pattern MARKET_ID_PATTERN = Pattern.compile("^2\\d{5}$");
    
    // Business limits
    private static final int MAX_QUANTITY = 1_000_000;
    private static final int MIN_QUANTITY = 1;
    private static final double MAX_PRICE = 1_000_000.0;
    private static final double MIN_PRICE = 0.01;
    
    public static void main(String[] args) {
        logger.info("=".repeat(60));
        logger.info("Starting FIX Broker...");
        logger.info("=".repeat(60));
        
        BrokerClient client = new BrokerClient();
        
        try {
            client.connect();
            
            String brokerId = client.getBrokerId();
            currentBrokerId = brokerId; // Store for prompt redisplay
            logger.info("Broker {} ready", brokerId);
            
            Thread receiverThread = new Thread(() -> receiveMessages(client));
            receiverThread.setDaemon(false);
            receiverThread.start();
            
            Thread.sleep(100);
            
            runCLI(client, brokerId);
            
            logger.info("Shutting down...");
            receiverThread.interrupt();
            client.close();
            
        } catch (IOException e) {
            logger.error("Failed to start: {}", e.getMessage());
            System.err.println("ERROR: Router not running?");
            System.exit(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        logger.info("Broker stopped");
    }
    
    private static void receiveMessages(BrokerClient client) {
        try {
            while (client.isConnected() && !Thread.currentThread().isInterrupted()) {
                String message = client.receiveMessage();
                if (message == null) break;
                
                handleIncoming(message);
            }
        } catch (IOException e) {
            if (client.isConnected()) {
                logger.error("Error receiving", e);
            }
        }
    }
    
    private static void runCLI(BrokerClient client, String brokerId) {
        Scanner scanner = new Scanner(System.in);
        
        printHelp();
        
        while (client.isConnected()) {
            System.out.print("\n" + brokerId + " > ");
            
            String input;
            try {
                input = scanner.nextLine().trim();
            } catch (Exception e) {
                logger.warn("Error reading input", e);
                continue;
            }
            
            if (input.isEmpty()) continue;
            
            try {
                processCommand(client, brokerId, input);
            } catch (NumberFormatException e) {
                System.out.println("ERROR: Invalid number format");
                System.out.println("   Quantity must be an integer (e.g., 100)");
                System.out.println("   Price must be a decimal (e.g., 150.50)");
                System.out.print(brokerId + " > ");
                System.out.flush();
            } catch (IOException e) {
                System.out.println("ERROR: Communication error: " + e.getMessage());
                logger.error("Error sending message", e);
                System.out.print(brokerId + " > ");
                System.out.flush();
            } catch (Exception e) {
                System.out.println("ERROR: Unexpected error: " + e.getMessage());
                logger.error("Unexpected error processing command", e);
                System.out.print(brokerId + " > ");
                System.out.flush();
            }
        }
    }
    
    private static void processCommand(BrokerClient client, String brokerId, String input) throws IOException {
        String[] parts = input.split("\\s+");
        if (parts.length == 0) return;
        
        String firstArg = parts[0].toLowerCase();
        
        // Check if it's a special command
        switch (firstArg) {
            case "help":
            case "h":
            case "?":
                printHelp();
                return;
                
            case "quit":
            case "exit":
            case "q":
                System.out.println("Goodbye!");
                System.exit(0);
                return;
        }
        
        // Otherwise, treat as order: <marketId> <buy|sell> <symbol> <qty> <price>
        if (parts.length < 5) {
            System.out.println("Usage: <marketId> <buy|sell> <symbol> <qty> <price>");
            System.out.println("Example: 200001 buy AAPL 100 150.50");
            System.out.println("Type 'help' for more information");
            return;
        }
        
        try {
            String marketId = validateMarketId(parts[0]);
            String side = validateSide(parts[1]);
            String symbol = validateSymbol(parts[2]);
            int qty = validateQuantity(parts[3]);
            double price = validatePrice(parts[4]);
            
            sendOrder(client, brokerId, marketId, side, symbol, qty, price);
            
        } catch (IllegalArgumentException e) {
            System.out.println("ERROR: " + e.getMessage());
            System.out.println("Usage: <marketId> <buy|sell> <symbol> <qty> <price>");
            System.out.println("Example: 200001 buy AAPL 100 150.50");
        }
    }
    
    // ========== VALIDATION METHODS ==========
    
    private static String validateSymbol(String symbol) {
        if (symbol == null || symbol.isEmpty()) {
            throw new IllegalArgumentException("Symbol cannot be empty");
        }
        
        String upperSymbol = symbol.toUpperCase();
        
        if (!SYMBOL_PATTERN.matcher(upperSymbol).matches()) {
            throw new IllegalArgumentException(
                String.format("Invalid symbol '%s'. Must be 1-10 uppercase letters (e.g., AAPL, GOOGL)", symbol)
            );
        }
        
        return upperSymbol;
    }
    
    private static int validateQuantity(String qtyStr) {
        int qty;
        try {
            qty = Integer.parseInt(qtyStr);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                String.format("Invalid quantity '%s'. Must be an integer", qtyStr)
            );
        }
        
        if (qty < MIN_QUANTITY) {
            throw new IllegalArgumentException(
                String.format("Quantity must be at least %d", MIN_QUANTITY)
            );
        }
        
        if (qty > MAX_QUANTITY) {
            throw new IllegalArgumentException(
                String.format("Quantity cannot exceed %,d", MAX_QUANTITY)
            );
        }
        
        return qty;
    }
    
    private static double validatePrice(String priceStr) {
        double price;
        try {
            price = Double.parseDouble(priceStr);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                String.format("Invalid price '%s'. Must be a number (e.g., 150.50)", priceStr)
            );
        }
        
        if (Double.isNaN(price) || Double.isInfinite(price)) {
            throw new IllegalArgumentException("Invalid price value");
        }
        
        if (price < MIN_PRICE) {
            throw new IllegalArgumentException(
                String.format("Price must be at least $%.2f", MIN_PRICE)
            );
        }
        
        if (price > MAX_PRICE) {
            throw new IllegalArgumentException(
                String.format("Price cannot exceed $%,.2f", MAX_PRICE)
            );
        }
        
        return price;
    }
    
    private static String validateMarketId(String marketId) {
        if (marketId == null || marketId.isEmpty()) {
            throw new IllegalArgumentException("Market ID cannot be empty");
        }
        
        if (!MARKET_ID_PATTERN.matcher(marketId).matches()) {
            throw new IllegalArgumentException(
                String.format("Invalid market ID '%s'. Must be 6 digits starting with 2 (e.g., 200001)", marketId)
            );
        }
        
        return marketId;
    }
    
    private static String validateSide(String side) {
        if (side == null || side.isEmpty()) {
            throw new IllegalArgumentException("Side cannot be empty");
        }
        
        String lowerSide = side.toLowerCase();
        
        if ("buy".equals(lowerSide) || "1".equals(lowerSide)) {
            return "1";
        } else if ("sell".equals(lowerSide) || "2".equals(lowerSide)) {
            return "2";
        } else {
            throw new IllegalArgumentException(
                String.format("Invalid side '%s'. Must be 'buy' or 'sell'", side)
            );
        }
    }
    
    // ========== SEND METHOD ==========
    
    private static void sendOrder(BrokerClient client, String brokerId, String marketId,
                                 String side, String symbol, int qty, double price) throws IOException {
        
        FixMessage order = FixMessageFactory.createNewOrderSingle(
            brokerId, marketId, symbol, side, qty, price
        );
        
        client.sendMessage(order.toString());
        
        String sideStr = "1".equals(side) ? "BUY" : "SELL";
        System.out.println(String.format("SENT %s: %s x%,d @ $%.2f → Market %s", 
                                        sideStr, symbol, qty, price, marketId));
    }
    
    // ========== HELP ==========
    
    private static void printHelp() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("               FIX BROKER - COMMAND LINE");
        System.out.println("=".repeat(60));
        System.out.println("Order Syntax:");
        System.out.println();
        System.out.println("  <marketId> <buy|sell> <symbol> <qty> <price>");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  200001 buy AAPL 100 150.50");
        System.out.println("  200001 sell GOOGL 50 2800");
        System.out.println("  200002 buy MSFT 75 380.25");
        System.out.println("  200001 sell TSLA 30 180.00");
        System.out.println();
        System.out.println("Other Commands:");
        System.out.println("  help | h | ?     Show this help");
        System.out.println("  quit | exit | q  Exit broker");
        System.out.println("=".repeat(60));
        System.out.println();
        System.out.println("Market ID Format:");
        System.out.println("  - 6 digits starting with 2");
        System.out.println("  - Example: 200001, 200002, 200003");
        System.out.println("  - First connected market is usually 200001");
        System.out.println();
        System.out.println("Order Parameters:");
        System.out.println("  Side:     buy or sell");
        System.out.println("  Symbol:   1-10 uppercase letters (AAPL, GOOGL, MSFT)");
        System.out.println("  Quantity: 1 to 1,000,000");
        System.out.println("  Price:    $0.01 to $1,000,000.00");
        System.out.println("=".repeat(60));
    }
    
    // ========== INCOMING MESSAGE HANDLING ==========
    
    private static void handleIncoming(String rawMessage) {
        if (rawMessage.startsWith("[")) {
            int idx = rawMessage.indexOf(']');
            if (idx > 0) rawMessage = rawMessage.substring(idx + 1).trim();
        }
        
        try {
            FixMessage msg = FixMessage.parse(rawMessage);
            
            if (FixTags.MSG_TYPE_EXECUTION_REPORT.equals(msg.getMsgType())) {
                displayReport(msg);
            } else if (rawMessage.contains("ERROR")) {
                System.out.println("\nERROR: " + rawMessage);
                redisplayPrompt();
            }
        } catch (Exception e) {
            if (rawMessage.contains("ERROR")) {
                System.out.println("\nERROR: " + rawMessage);
                redisplayPrompt();
            }
        }
    }
    
    private static void redisplayPrompt() {
        if (currentBrokerId != null) {
            System.out.print(currentBrokerId + " > ");
            System.out.flush();
        }
    }
    
    private static void displayReport(FixMessage msg) {
        String ordStatus = msg.getField(FixTags.ORD_STATUS);
        String symbol = msg.getSymbol();
        String qty = msg.getField(FixTags.ORDER_QTY);
        String price = msg.getField(FixTags.PRICE);
        String text = msg.getField(FixTags.TEXT);
        String marketId = msg.getSenderCompId();
        
        System.out.println("\n" + "─".repeat(60));
        
        if (FixTags.ORD_STATUS_FILLED.equals(ordStatus)) {
            System.out.println("ORDER FILLED - Market " + marketId);
            System.out.println("   Symbol: " + symbol);
            if (qty != null) System.out.println("   Qty:    " + qty);
            if (price != null) System.out.println("   Price:  $" + price);
        } else if (FixTags.ORD_STATUS_REJECTED.equals(ordStatus)) {
            System.out.println("ORDER REJECTED - Market " + marketId);
            System.out.println("   Symbol: " + symbol);
            if (text != null) System.out.println("   Reason: " + text);
        }
        
        System.out.println("─".repeat(60));
        redisplayPrompt();
    }
}