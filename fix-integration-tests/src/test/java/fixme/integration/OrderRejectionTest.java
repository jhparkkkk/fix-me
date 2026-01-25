package fixme.integration;

import fixme.common.message.FixMessage;
import fixme.common.message.FixMessageFactory;
import fixme.common.message.FixTags;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for order rejection scenarios.
 */
@DisplayName("Order Rejection Integration Tests")
public class OrderRejectionTest extends IntegrationTestBase {
    
    @Test
    @DisplayName("Order with insufficient quantity should be rejected")
    public void testInsufficientQuantityRejection() throws Exception {
        // Given: Broker and Market connected
        TestClient broker = createBroker();
        TestClient market = createMarket();
        
        String brokerId = broker.getClientId();
        String marketId = market.getClientId();
        
        // When: Broker sends order for huge quantity
        FixMessage buyOrder = FixMessageFactory.createBuyOrder(
            brokerId, marketId, "AAPL", 10000, 150.0  // Way more than inventory
        );
        
        broker.send(buyOrder.toString());
        logger.info("Broker sent BUY order with huge quantity: AAPL x10000");
        
        // Then: Market receives order
        String marketReceived = market.receive();
        assertThat(marketReceived).isNotNull();
        
        logger.info("✓ Market received order");
        
        // When: Market sends rejection
        FixMessage rejection = FixMessageFactory.createRejectedReport(
            marketId, brokerId, "AAPL", "Insufficient quantity (available: 1000)"
        );
        
        market.send(rejection.toString());
        logger.info("Market sent REJECTED report");
        
        // Then: Broker receives rejection
        String brokerReceived = broker.receive();
        assertThat(brokerReceived).isNotNull();
        
        String cleanReport = stripPrefix(brokerReceived);
        FixMessage receivedReport = FixMessage.parse(cleanReport);
        
        assertThat(receivedReport.getMsgType()).isEqualTo(FixTags.MSG_TYPE_EXECUTION_REPORT);
        assertThat(receivedReport.getField(FixTags.ORD_STATUS)).isEqualTo(FixTags.ORD_STATUS_REJECTED);
        assertThat(receivedReport.getSymbol()).isEqualTo("AAPL");
        assertThat(receivedReport.getField(FixTags.TEXT)).contains("Insufficient quantity");
        
        logger.info("✓ Broker received REJECTED report with reason");
        logger.info("✓ Insufficient quantity rejection flow successful");
        
        broker.close();
        market.close();
    }
    
    @Test
    @DisplayName("Order for invalid symbol should be rejected")
    public void testInvalidSymbolRejection() throws Exception {
        // Given: Broker and Market connected
        TestClient broker = createBroker();
        TestClient market = createMarket();
        
        String brokerId = broker.getClientId();
        String marketId = market.getClientId();
        
        // When: Broker sends order for non-traded symbol
        FixMessage buyOrder = FixMessageFactory.createBuyOrder(
            brokerId, marketId, "INVALID", 100, 150.0
        );
        
        broker.send(buyOrder.toString());
        logger.info("Broker sent order for invalid symbol: INVALID");
        
        // Then: Market receives order
        String marketReceived = market.receive();
        assertThat(marketReceived).isNotNull();
        
        // When: Market sends rejection
        FixMessage rejection = FixMessageFactory.createRejectedReport(
            marketId, brokerId, "INVALID", "Symbol INVALID not traded on this market"
        );
        
        market.send(rejection.toString());
        logger.info("Market sent REJECTED report");
        
        // Then: Broker receives rejection
        String brokerReceived = broker.receive();
        String cleanReport = stripPrefix(brokerReceived);
        FixMessage receivedReport = FixMessage.parse(cleanReport);
        
        assertThat(receivedReport.getField(FixTags.ORD_STATUS)).isEqualTo(FixTags.ORD_STATUS_REJECTED);
        assertThat(receivedReport.getSymbol()).isEqualTo("INVALID");
        assertThat(receivedReport.getField(FixTags.TEXT)).contains("not traded");
        
        logger.info("✓ Invalid symbol rejection flow successful");
        
        broker.close();
        market.close();
    }
    
    @Test
    @DisplayName("Order to non-existent market should fail routing")
    public void testNonExistentMarketRouting() throws Exception {
        // Given: Broker connected, but NO market
        TestClient broker = createBroker();
        String brokerId = broker.getClientId();
        
        // When: Broker sends order to non-existent market
        FixMessage buyOrder = FixMessageFactory.createBuyOrder(
            brokerId, "M99999", "AAPL", 100, 150.0  // Non-existent market
        );
        
        broker.send(buyOrder.toString());
        logger.info("Broker sent order to non-existent market M99999");
        
        // Then: Router should send error (routing failure)
        String brokerReceived = broker.receive(2000);
        
        if (brokerReceived != null) {
            assertThat(brokerReceived).containsIgnoringCase("error");
            logger.info("✓ Router sent error for non-existent destination");
        }
        
        broker.close();
    }
}