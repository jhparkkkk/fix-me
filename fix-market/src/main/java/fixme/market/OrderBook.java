package fixme.market;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Order book that maintains available instruments and their quantities.
 * Thread-safe using ConcurrentHashMap.
 */
public class OrderBook {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderBook.class);
    
    private final Map<String, Integer> inventory;
    
    public OrderBook() {
        this.inventory = new ConcurrentHashMap<>();
        initializeInventory();
    }
    
    /**
     * Initializes the inventory with default instruments.
     */
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
    
    /**
     * Checks if an order can be executed.
     * 
     * @param symbol the instrument symbol
     * @param quantity the requested quantity
     * @param isBuy true if buy order, false if sell
     * @return true if the order can be executed
     */
    public synchronized boolean canExecute(String symbol, int quantity, boolean isBuy) {
        if (!inventory.containsKey(symbol)) {
            logger.debug("Symbol {} not traded on this market", symbol);
            return false;
        }
        
        int available = inventory.get(symbol);
        
        if (isBuy) {
            // For buy orders, broker wants to buy from market
            // Market must have enough shares to sell
            boolean canFill = available >= quantity;
            logger.debug("Buy order: {} shares available, {} requested -> {}", 
                        available, quantity, canFill ? "OK" : "REJECT");
            return canFill;
        } else {
            // For sell orders, broker wants to sell to market
            // Market always accepts (buying from broker)
            logger.debug("Sell order: always accept (market buys from broker)");
            return true;
        }
    }
    
    /**
     * Executes an order by updating the inventory.
     * This method should only be called after canExecute returns true.
     * 
     * @param symbol the instrument symbol
     * @param quantity the quantity to execute
     * @param isBuy true if buy order, false if sell
     */
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
    
    /**
     * Returns the available quantity for a symbol.
     * 
     * @param symbol the instrument symbol
     * @return available quantity, or 0 if not traded
     */
    public int getAvailable(String symbol) {
        return inventory.getOrDefault(symbol, 0);
    }
    
    /**
     * Returns true if the symbol is traded on this market.
     */
    public boolean isTradedSymbol(String symbol) {
        return inventory.containsKey(symbol);
    }
    
    /**
     * Displays the current inventory.
     */
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