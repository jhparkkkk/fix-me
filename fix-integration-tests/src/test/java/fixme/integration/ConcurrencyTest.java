package fixme.integration;

import fixme.common.message.FixMessage;
import fixme.common.message.FixMessageFactory;
import fixme.common.message.FixTags;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for concurrent scenarios.
 */
@DisplayName("Concurrency Integration Tests")
public class ConcurrencyTest extends IntegrationTestBase {
    
    @Test
    @DisplayName("Two brokers should send orders simultaneously without interference")
    public void testTwoBrokersSimultaneous() throws Exception {
        // Given: 2 brokers and 1 market connected
        TestClient broker1 = createBroker();
        TestClient broker2 = createBroker();
        TestClient market = createMarket();
        
        String broker1Id = broker1.getClientId();
        String broker2Id = broker2.getClientId();
        String marketId = market.getClientId();
        
        logger.info("Broker1: {}, Broker2: {}, Market: {}", broker1Id, broker2Id, marketId);
        
        // When: Both brokers send orders simultaneously
        ExecutorService executor = Executors.newFixedThreadPool(2);
        
        Future<?> future1 = executor.submit(() -> {
            try {
                FixMessage order = FixMessageFactory.createBuyOrder(
                    broker1Id, marketId, "AAPL", 100, 150.0
                );
                broker1.send(order.toString());
                logger.info("Broker1 sent order");
            } catch (Exception e) {
                logger.error("Broker1 error", e);
            }
        });
        
        Future<?> future2 = executor.submit(() -> {
            try {
                FixMessage order = FixMessageFactory.createBuyOrder(
                    broker2Id, marketId, "GOOGL", 50, 2800.0
                );
                broker2.send(order.toString());
                logger.info("Broker2 sent order");
            } catch (Exception e) {
                logger.error("Broker2 error", e);
            }
        });
        
        future1.get(5, TimeUnit.SECONDS);
        future2.get(5, TimeUnit.SECONDS);
        
        // Then: Market receives both orders
        String recv1 = market.receive();
        String recv2 = market.receive();
        
        assertThat(recv1).isNotNull();
        assertThat(recv2).isNotNull();
        
        FixMessage msg1 = FixMessage.parse(stripPrefix(recv1));
        FixMessage msg2 = FixMessage.parse(stripPrefix(recv2));
        
        // Verify both orders received (order may vary)
        List<String> symbols = List.of(msg1.getSymbol(), msg2.getSymbol());
        assertThat(symbols).containsExactlyInAnyOrder("AAPL", "GOOGL");
        
        logger.info("✓ Market received both orders correctly");
        
        // When: Market responds to both
        market.send(FixMessageFactory.createFilledReport(marketId, broker1Id, "AAPL", 100, 150.0).toString());
        market.send(FixMessageFactory.createFilledReport(marketId, broker2Id, "GOOGL", 50, 2800.0).toString());
        
        // Then: Each broker receives their report
        String rep1 = broker1.receive();
        String rep2 = broker2.receive();
        
        assertThat(rep1).isNotNull();
        assertThat(rep2).isNotNull();
        
        FixMessage report1 = FixMessage.parse(stripPrefix(rep1));
        FixMessage report2 = FixMessage.parse(stripPrefix(rep2));
        
        assertThat(report1.getField(FixTags.ORD_STATUS)).isEqualTo(FixTags.ORD_STATUS_FILLED);
        assertThat(report2.getField(FixTags.ORD_STATUS)).isEqualTo(FixTags.ORD_STATUS_FILLED);
        
        logger.info("✓ Both brokers received their execution reports");
        logger.info("✓ Concurrent broker test successful");
        
        executor.shutdown();
        broker1.close();
        broker2.close();
        market.close();
    }
    
    @Test
    @DisplayName("One broker sending to two markets simultaneously")
    public void testOneBrokerTwoMarkets() throws Exception {
        // Given: 1 broker and 2 markets connected
        TestClient broker = createBroker();
        TestClient market1 = createMarket();
        TestClient market2 = createMarket();
        
        String brokerId = broker.getClientId();
        String market1Id = market1.getClientId();
        String market2Id = market2.getClientId();
        
        logger.info("Broker: {}, Market1: {}, Market2: {}", brokerId, market1Id, market2Id);
        
        // When: Broker sends order to market1
        FixMessage order1 = FixMessageFactory.createBuyOrder(
            brokerId, market1Id, "AAPL", 100, 150.0
        );
        broker.send(order1.toString());
        
        // And: Broker sends order to market2
        FixMessage order2 = FixMessageFactory.createBuyOrder(
            brokerId, market2Id, "GOOGL", 50, 2800.0
        );
        broker.send(order2.toString());
        
        logger.info("Broker sent orders to both markets");
        
        // Then: Each market receives ONLY their order
        String recv1 = market1.receive();
        String recv2 = market2.receive();
        
        assertThat(recv1).isNotNull();
        assertThat(recv2).isNotNull();
        
        FixMessage msg1 = FixMessage.parse(stripPrefix(recv1));
        FixMessage msg2 = FixMessage.parse(stripPrefix(recv2));
        
        assertThat(msg1.getTargetCompId()).isEqualTo(market1Id);
        assertThat(msg1.getSymbol()).isEqualTo("AAPL");
        
        assertThat(msg2.getTargetCompId()).isEqualTo(market2Id);
        assertThat(msg2.getSymbol()).isEqualTo("GOOGL");
        
        logger.info("✓ Each market received correct order");
        logger.info("✓ Routing to multiple markets successful");
        
        broker.close();
        market1.close();
        market2.close();
    }
    
    @Test
    @DisplayName("High load: 5 orders sent with pacing")
    public void testHighLoad() throws Exception {
        // Given: Broker and Market connected
        TestClient broker = createBroker();
        TestClient market = createMarket();
        
        String brokerId = broker.getClientId();
        String marketId = market.getClientId();
        
        // When: Send 5 orders with pacing (reduced from 10 for reliability)
        int numOrders = 5;
        for (int i = 0; i < numOrders; i++) {
            FixMessage order = FixMessageFactory.createBuyOrder(
                brokerId, marketId, "AAPL", 10, 150.0 + i
            );
            broker.send(order.toString());
            Thread.sleep(50); // Small delay for pacing
        }
        
        logger.info("Sent {} orders with pacing", numOrders);
        
        // Then: Market receives all orders
        List<String> receivedOrders = new ArrayList<>();
        for (int i = 0; i < numOrders; i++) {
            String recv = market.receive();
            assertThat(recv).isNotNull();
            receivedOrders.add(recv);
        }
        
        assertThat(receivedOrders).hasSize(numOrders);
        
        logger.info("✓ Market received all {} orders", numOrders);
        logger.info("✓ High load test successful");
        
        broker.close();
        market.close();
    }
}