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
    
    // Validation patterns
    private static final Pattern SYMBOL_PATTERN = Pattern.compile("^[A-Z]{1,10}$");
    private static final Pattern MARKET_ID_PATTERN = Pattern.compile("^[BM]\\d{5}$");
    
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
                System.out.println("❌ Invalid number format");
                System.out.println("   Quantity must be an integer (e.g., 100)");
                System.out.println("   Price must be a decimal (e.g., 150.50)");
            } catch (IllegalArgumentException e) {
                System.out.println("❌ " + e.getMessage());
            } catch (IOException e) {
                System.out.println("❌ Communication error: " + e.getMessage());
                logger.error("Error sending message", e);
            } catch (Exception e) {
                System.out.println("❌ Unexpected error: " + e.getMessage());
                logger.error("Unexpected error processing command", e);
            }
        }
    }
    
    private static void processCommand(BrokerClient client, String brokerId, String input) throws IOException {
        String[] parts = input.split("\\s+");
        if (parts.length == 0) return;
        
        String command = parts[0].toLowerCase();
        
        switch (command) {
            case "buy":
                if (parts.length < 4) {
                    System.out.println("Usage: buy <symbol> <quantity> <price>");
                    System.out.println("Example: buy AAPL 100 150.50");
                    return;
                }
                
                String buySymbol = validateSymbol(parts[1]);
                int buyQty = validateQuantity(parts[2]);
                double buyPrice = validatePrice(parts[3]);
                
                sendBuy(client, brokerId, buySymbol, buyQty, buyPrice);
                break;
                
            case "sell":
                if (parts.length < 4) {
                    System.out.println("Usage: sell <symbol> <quantity> <price>");
                    System.out.println("Example: sell AAPL 50 150.50");
                    return;
                }
                
                String sellSymbol = validateSymbol(parts[1]);
                int sellQty = validateQuantity(parts[2]);
                double sellPrice = validatePrice(parts[3]);
                
                sendSell(client, brokerId, sellSymbol, sellQty, sellPrice);
                break;
                
            case "market":
            case "to":
                if (parts.length < 6) {
                    System.out.println("Usage: market <marketId> <buy|sell> <symbol> <qty> <price>");
                    System.out.println("Example: market M00001 buy AAPL 100 150.50");
                    return;
                }
                
                String marketId = validateMarketId(parts[1]);
                String side = validateSide(parts[2]);
                String symbol = validateSymbol(parts[3]);
                int qty = validateQuantity(parts[4]);
                double price = validatePrice(parts[5]);
                
                sendToMarket(client, brokerId, marketId, side, symbol, qty, price);
                break;
                
            case "help":
            case "h":
            case "?":
                printHelp();
                break;
                
            case "quit":
            case "exit":
            case "q":
                System.out.println("Goodbye!");
                System.exit(0);
                break;
                
            default:
                System.out.println("Unknown command: " + command);
                System.out.println("Type 'help' for available commands");
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
        
        String upperMarketId = marketId.toUpperCase();
        
        if (!MARKET_ID_PATTERN.matcher(upperMarketId).matches()) {
            throw new IllegalArgumentException(
                String.format("Invalid market ID '%s'. Must be format M00001 or B00001", marketId)
            );
        }
        
        return upperMarketId;
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
    
    // ========== SEND METHODS ==========
    
    private static void printHelp() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("               FIX BROKER - COMMAND LINE");
        System.out.println("=".repeat(60));
        System.out.println("Commands:");
        System.out.println("  buy <symbol> <qty> <price>");
        System.out.println("      Send buy order to default market (M00001)");
        System.out.println("      Example: buy AAPL 100 150.50");
        System.out.println();
        System.out.println("  sell <symbol> <qty> <price>");
        System.out.println("      Send sell order to default market (M00001)");
        System.out.println("      Example: sell GOOGL 20 2800");
        System.out.println();
        System.out.println("  market <marketId> <buy|sell> <symbol> <qty> <price>");
        System.out.println("      Send order to specific market");
        System.out.println("      Example: market M00001 buy MSFT 75 380");
        System.out.println();
        System.out.println("  help | h | ?");
        System.out.println("      Show this help");
        System.out.println();
        System.out.println("  quit | exit | q");
        System.out.println("      Exit broker");
        System.out.println("=".repeat(60));
        System.out.println();
        System.out.println("Limits:");
        System.out.println("  Quantity: 1 to 1,000,000");
        System.out.println("  Price:    $0.01 to $1,000,000.00");
        System.out.println("  Symbol:   1-10 uppercase letters");
        System.out.println("=".repeat(60));
    }
    
    private static void sendBuy(BrokerClient client, String brokerId, 
                               String symbol, int qty, double price) throws IOException {
        FixMessage order = FixMessageFactory.createBuyOrder(
            brokerId, "M00001", symbol, qty, price
        );
        
        client.sendMessage(order.toString());
        System.out.println(String.format("📤 BUY:  %s x%d @ $%.2f → M00001", symbol, qty, price));
    }
    
    private static void sendSell(BrokerClient client, String brokerId,
                                String symbol, int qty, double price) throws IOException {
        FixMessage order = FixMessageFactory.createSellOrder(
            brokerId, "M00001", symbol, qty, price
        );
        
        client.sendMessage(order.toString());
        System.out.println(String.format("📤 SELL: %s x%d @ $%.2f → M00001", symbol, qty, price));
    }
    
    private static void sendToMarket(BrokerClient client, String brokerId, String marketId,
                                    String side, String symbol, int qty, double price) throws IOException {
        FixMessage order = FixMessageFactory.createNewOrderSingle(
            brokerId, marketId, symbol, side, qty, price
        );
        
        client.sendMessage(order.toString());
        
        String sideStr = "1".equals(side) ? "BUY" : "SELL";
        System.out.println(String.format("📤 %s: %s x%d @ $%.2f → %s", 
                                        sideStr, symbol, qty, price, marketId));
    }
    
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
                System.out.println("\n⚠️  " + rawMessage);
            }
        } catch (Exception e) {
            if (rawMessage.contains("ERROR")) {
                System.out.println("\n⚠️  " + rawMessage);
            }
        }
    }
    
    private static void displayReport(FixMessage msg) {
        String ordStatus = msg.getField(FixTags.ORD_STATUS);
        String symbol = msg.getSymbol();
        String qty = msg.getField(FixTags.ORDER_QTY);
        String price = msg.getField(FixTags.PRICE);
        String text = msg.getField(FixTags.TEXT);
        
        System.out.println("\n" + "─".repeat(50));
        
        if (FixTags.ORD_STATUS_FILLED.equals(ordStatus)) {
            System.out.println("✅ ORDER FILLED");
            System.out.println("   Symbol: " + symbol);
            if (qty != null) System.out.println("   Qty:    " + qty);
            if (price != null) System.out.println("   Price:  $" + price);
        } else if (FixTags.ORD_STATUS_REJECTED.equals(ordStatus)) {
            System.out.println("❌ ORDER REJECTED");
            System.out.println("   Symbol: " + symbol);
            if (text != null) System.out.println("   Reason: " + text);
        }
        
        System.out.println("─".repeat(50));
    }
}