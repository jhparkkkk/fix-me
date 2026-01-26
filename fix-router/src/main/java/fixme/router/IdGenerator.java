package fixme.router;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Generates unique 6-digit IDs for connected clients.
 * Format: [1|2][5-digit number]
 *   - Brokers: 100001, 100002, 100003, etc.
 *   - Markets: 200001, 200002, 200003, etc.
 */
public class IdGenerator {

    private final AtomicInteger brokerCounter;
    private final AtomicInteger marketCounter;

    private static final int MAX_ID = 99999;

    public IdGenerator() {
        this.brokerCounter = new AtomicInteger(0);
        this.marketCounter = new AtomicInteger(0);
    }

    public String generateId(ComponentType type) {
        AtomicInteger counter = getCounterForType(type);

        int id = counter.incrementAndGet();
        if (id > MAX_ID) {
            throw new IllegalStateException("Maximum number of IDs reached for type: " + type);
        }
        
        // Format: [prefix][5-digit number]
        return String.format("%s%05d", type.getPrefix(), id);
    }

    private AtomicInteger getCounterForType(ComponentType type) {
        switch (type) {
            case BROKER:
                return brokerCounter;
            case MARKET:
                return marketCounter;
            default:
                throw new IllegalArgumentException("Unknown ComponentType: " + type);
        }
    }

    public int getCurrentCount(ComponentType type) {
        return getCounterForType(type).get();
    }

    public void reset(ComponentType type) {
        getCounterForType(type).set(0);
    }
}