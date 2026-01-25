package fixme.integration;

import fixme.common.message.FixMessage;
import fixme.common.message.FixMessageFactory;
import fixme.common.message.FixTags;
import fixme.market.OrderBook;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for successful order execution.
 */
@DisplayName("Order Execution Integration Tests")
public class OrderExecutionTest extends IntegrationTestBase {
    
    @Test
    @DisplayName("Buy order should be routed from Broker to Market and filled")
    public void testBuyOrderFilled() throws Exception {
        // Given: Broker and Market connected
        TestClient broker = createBroker();
        TestClient market = createMarket();
        
        String brokerId = broker.getClientId();
        String marketId = market.getClientId();
        
        // When: Broker sends buy order
        FixMessage buyOrder = FixMessageFactory.createBuyOrder(
            brokerId, marketId, "AAPL", 100, 150.50
        );
        
        broker.send(buyOrder.toString());
        logger.info("Broker sent BUY order: AAPL x100 @ $150.50");
        
        // Then: Market receives the order
        String marketReceived = market.receive();
        assertThat(marketReceived).isNotNull();
        
        String cleanMessage = stripPrefix(marketReceived);
        FixMessage receivedOrder = FixMessage.parse(cleanMessage);
        
        assertThat(receivedOrder.getSenderCompId()).isEqualTo(brokerId);
        assertThat(receivedOrder.getTargetCompId()).isEqualTo(marketId);
        assertThat(receivedOrder.getSymbol()).isEqualTo("AAPL");
        assertThat(receivedOrder.getField(FixTags.ORDER_QTY)).isEqualTo("100");
        
        logger.info("✓ Market received order correctly");
        
        // When: Market sends ExecutionReport (filled)
        FixMessage executionReport = FixMessageFactory.createFilledReport(
            marketId, brokerId, "AAPL", 100, 150.50
        );
        
        market.send(executionReport.toString());
        logger.info("Market sent FILLED report");
        
        // Then: Broker receives ExecutionReport
        String brokerReceived = broker.receive();
        assertThat(brokerReceived).isNotNull();
        
        String cleanReport = stripPrefix(brokerReceived);
        FixMessage receivedReport = FixMessage.parse(cleanReport);
        
        assertThat(receivedReport.getMsgType()).isEqualTo(FixTags.MSG_TYPE_EXECUTION_REPORT);
        assertThat(receivedReport.getField(FixTags.ORD_STATUS)).isEqualTo(FixTags.ORD_STATUS_FILLED);
        assertThat(receivedReport.getSymbol()).isEqualTo("AAPL");
        assertThat(receivedReport.getField(FixTags.ORDER_QTY)).isEqualTo("100");
        
        logger.info("✓ Broker received FILLED report");
        logger.info("✓ Complete buy order flow successful");
        
        broker.close();
        market.close();
    }
    
    @Test
    @DisplayName("Sell order should be routed and filled")
    public void testSellOrderFilled() throws Exception {
        // Given: Broker and Market connected
        TestClient broker = createBroker();
        TestClient market = createMarket();
        
        String brokerId = broker.getClientId();
        String marketId = market.getClientId();
        
        // When: Broker sends sell order
        FixMessage sellOrder = FixMessageFactory.createSellOrder(
            brokerId, marketId, "GOOGL", 50, 2800.0
        );
        
        broker.send(sellOrder.toString());
        logger.info("Broker sent SELL order: GOOGL x50 @ $2800");
        
        // Then: Market receives the order
        String marketReceived = market.receive();
        assertThat(marketReceived).isNotNull();
        
        String cleanMessage = stripPrefix(marketReceived);
        FixMessage receivedOrder = FixMessage.parse(cleanMessage);
        
        assertThat(receivedOrder.getSymbol()).isEqualTo("GOOGL");
        assertThat(receivedOrder.getField(FixTags.SIDE)).isEqualTo(FixTags.SIDE_SELL);
        assertThat(receivedOrder.getField(FixTags.ORDER_QTY)).isEqualTo("50");
        
        logger.info("✓ Market received sell order");
        
        // When: Market sends filled report
        FixMessage executionReport = FixMessageFactory.createFilledReport(
            marketId, brokerId, "GOOGL", 50, 2800.0
        );
        
        market.send(executionReport.toString());
        
        // Then: Broker receives it
        String brokerReceived = broker.receive();
        assertThat(brokerReceived).isNotNull();
        
        String cleanReport = stripPrefix(brokerReceived);
        FixMessage receivedReport = FixMessage.parse(cleanReport);
        
        assertThat(receivedReport.getField(FixTags.ORD_STATUS)).isEqualTo(FixTags.ORD_STATUS_FILLED);
        assertThat(receivedReport.getSymbol()).isEqualTo("GOOGL");
        
        logger.info("✓ Complete sell order flow successful");
        
        broker.close();
        market.close();
    }
    
    @Test
    @DisplayName("Multiple orders should be routed correctly")
    public void testMultipleOrders() throws Exception {
        // Given: Broker and Market connected
        TestClient broker = createBroker();
        TestClient market = createMarket();
        
        String brokerId = broker.getClientId();
        String marketId = market.getClientId();
        
        // When: Broker sends 3 orders with small delays
        FixMessage order1 = FixMessageFactory.createBuyOrder(brokerId, marketId, "AAPL", 100, 150.0);
        broker.send(order1.toString());
        Thread.sleep(100); // Small delay between sends
        
        FixMessage order2 = FixMessageFactory.createBuyOrder(brokerId, marketId, "GOOGL", 20, 2800.0);
        broker.send(order2.toString());
        Thread.sleep(100);
        
        FixMessage order3 = FixMessageFactory.createSellOrder(brokerId, marketId, "MSFT", 50, 380.0);
        broker.send(order3.toString());
        
        logger.info("Broker sent 3 orders");
        
        // Then: Market receives all 3 orders
        String recv1 = market.receive();
        String recv2 = market.receive();
        String recv3 = market.receive();
        
        assertThat(recv1).isNotNull();
        assertThat(recv2).isNotNull();
        assertThat(recv3).isNotNull();
        
        // Parse and verify
        FixMessage msg1 = FixMessage.parse(stripPrefix(recv1));
        FixMessage msg2 = FixMessage.parse(stripPrefix(recv2));
        FixMessage msg3 = FixMessage.parse(stripPrefix(recv3));
        
        assertThat(msg1.getSymbol()).isEqualTo("AAPL");
        assertThat(msg2.getSymbol()).isEqualTo("GOOGL");
        assertThat(msg3.getSymbol()).isEqualTo("MSFT");
        assertThat(msg3.getField(FixTags.SIDE)).isEqualTo(FixTags.SIDE_SELL);
        
        logger.info("✓ All 3 orders routed correctly");
        
        // When: Market responds to all with delays
        market.send(FixMessageFactory.createFilledReport(marketId, brokerId, "AAPL", 100, 150.0).toString());
        Thread.sleep(100);
        market.send(FixMessageFactory.createFilledReport(marketId, brokerId, "GOOGL", 20, 2800.0).toString());
        Thread.sleep(100);
        market.send(FixMessageFactory.createFilledReport(marketId, brokerId, "MSFT", 50, 380.0).toString());
        
        // Then: Broker receives all 3 reports
        String rep1 = broker.receive();
        String rep2 = broker.receive();
        String rep3 = broker.receive();
        
        assertThat(rep1).isNotNull();
        assertThat(rep2).isNotNull();
        assertThat(rep3).isNotNull();
        
        logger.info("✓ All 3 execution reports received");
        logger.info("✓ Multiple orders flow successful");
        
        broker.close();
        market.close();
    }
}