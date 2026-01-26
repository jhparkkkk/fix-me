package fixme.market;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Order book that maintains available instruments and their quantities.
 */
public class OrderBook {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderBook.class);
    
    private final Map<String, Integer> inventory;
    
    public OrderBook() {
        this.inventory = new ConcurrentHashMap<>();
        initializeInventory();
    }
    
    private void initializeInventory() {
        inventory.put("AAPL", 1000);
        inventory.put("GOOGL", 500);
        inventory.put("MSFT", 750);
        inventory.put("AMZN", 300);
        inventory.put("TSLA", 200);
        
        logger.info("Initialized inventory with {} instruments", inventory.size());
        inventory.forEach((symbol, qty) -> 
            logger.info("  {}: {} shares", symbol, qty)
        );
    }
    
    public synchronized boolean canExecute(String symbol, int quantity, boolean isBuy) {
        if (!inventory.containsKey(symbol)) {
            logger.debug("Symbol {} not traded on this market", symbol);
            return false;
        }
        
        int available = inventory.get(symbol);
        
        if (isBuy) {
            boolean canFill = available >= quantity;
            logger.debug("Buy order: {} shares available, {} requested -> {}", 
                        available, quantity, canFill ? "OK" : "REJECT");
            return canFill;
        } else {
            logger.debug("Sell order: always accept (market buys from broker)");
            return true;
        }
    }
    
    public synchronized void execute(String symbol, int quantity, boolean isBuy) {
        int current = inventory.getOrDefault(symbol, 0);
        int newQuantity;
        
        if (isBuy) {
            // Broker buys, market sells -> reduce inventory
            newQuantity = current - quantity;
            logger.info("Executed BUY: {} shares of {} (inventory: {} -> {})", 
                       quantity, symbol, current, newQuantity);
        } else {
            // Broker sells, market buys -> increase inventory
            newQuantity = current + quantity;
            logger.info("Executed SELL: {} shares of {} (inventory: {} -> {})", 
                       quantity, symbol, current, newQuantity);
        }
        
        inventory.put(symbol, newQuantity);
    }
    
    public int getAvailable(String symbol) {
        return inventory.getOrDefault(symbol, 0);
    }
    
    public boolean isTradedSymbol(String symbol) {
        return inventory.containsKey(symbol);
    }
    
    public void displayInventory() {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("           MARKET INVENTORY");
        System.out.println("=".repeat(50));
        
        inventory.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry -> 
                System.out.printf("  %-10s %,6d shares%n", entry.getKey(), entry.getValue())
            );
        
        System.out.println("=".repeat(50));
    }
}