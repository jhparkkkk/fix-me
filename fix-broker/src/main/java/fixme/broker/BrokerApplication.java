package fixme.broker;

import fixme.common.message.FixMessage;
import fixme.common.message.FixMessageFactory;
import fixme.common.message.FixTags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Scanner;

public class BrokerApplication {
    
    private static final Logger logger = LoggerFactory.getLogger(BrokerApplication.class);
    
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
            String input = scanner.nextLine().trim();
            
            if (input.isEmpty()) continue;
            
            try {
                processCommand(client, brokerId, input);
            } catch (Exception e) {
                System.out.println("❌ Error: " + e.getMessage());
                System.out.println("   Type 'help' for usage");
            }
        }
    }
    
    private static void processCommand(BrokerClient client, String brokerId, String input) throws IOException {
        String[] parts = input.toLowerCase().split("\\s+");
        String command = parts[0];
        
        switch (command) {
            case "buy":
                if (parts.length < 4) {
                    System.out.println("Usage: buy <symbol> <quantity> <price>");
                    System.out.println("Example: buy AAPL 100 150.50");
                    return;
                }
                sendBuy(client, brokerId, parts[1].toUpperCase(), 
                       Integer.parseInt(parts[2]), 
                       Double.parseDouble(parts[3]));
                break;
                
            case "sell":
                if (parts.length < 4) {
                    System.out.println("Usage: sell <symbol> <quantity> <price>");
                    System.out.println("Example: sell AAPL 50 150.50");
                    return;
                }
                sendSell(client, brokerId, parts[1].toUpperCase(),
                        Integer.parseInt(parts[2]),
                        Double.parseDouble(parts[3]));
                break;
                
            case "market":
            case "to":
                if (parts.length < 5) {
                    System.out.println("Usage: market <marketId> <buy|sell> <symbol> <qty> <price>");
                    System.out.println("Example: market M00002 buy AAPL 100 150.50");
                    return;
                }
                String marketId = parts[1].toUpperCase();
                String side = parts[2].equalsIgnoreCase("buy") ? "1" : "2";
                sendToMarket(client, brokerId, marketId, side,
                           parts[3].toUpperCase(),
                           Integer.parseInt(parts[4]),
                           Double.parseDouble(parts[5]));
                break;
                
            case "help":
            case "h":
            case "?":
                printHelp();
                break;
                
            case "quit":
            case "exit":
            case "q":
                return;
                
            default:
                System.out.println("Unknown command: " + command);
                System.out.println("Type 'help' for available commands");
        }
    }
    
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
        System.out.println("      Example: market M00002 buy MSFT 75 380");
        System.out.println();
        System.out.println("  help | h | ?");
        System.out.println("      Show this help");
        System.out.println();
        System.out.println("  quit | exit | q");
        System.out.println("      Exit broker");
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