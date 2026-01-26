package fixme.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for connection establishment.
 */
@DisplayName("Connection Integration Tests")
public class ConnectionTest extends IntegrationTestBase {
    @Test
    @DisplayName("Broker should connect and receive 6-digit ID starting with 1")
    public void testBrokerConnection() throws Exception {
        // When: Broker connects
        TestClient broker = createBroker();
        
        // Then: Broker receives 6-digit ID starting with 1
        assertThat(broker.getClientId())
            .isNotNull()
            .matches("^1\\d{5}$")  // 6 digits starting with 1
            .hasSize(6);
    
        logger.info("✓ Broker received valid 6-digit ID: {}", broker.getClientId());
    
        broker.close();
    }

    @Test
    @DisplayName("Market should connect and receive 6-digit ID starting with 2")
    public void testMarketConnection() throws Exception {
        // When: Market connects
        TestClient market = createMarket();
    
        // Then: Market receives 6-digit ID starting with 2
        assertThat(market.getClientId())
            .isNotNull()
            .matches("^2\\d{5}$")  // 6 digits starting with 2
            .hasSize(6);
    
        logger.info("✓ Market received valid 6-digit ID: {}", market.getClientId());
    
        market.close();
    }

    @Test
    @DisplayName("Multiple brokers should receive unique 6-digit IDs")
    public void testMultipleBrokers() throws Exception {
        // When: 3 brokers connect
        TestClient broker1 = createBroker();
        TestClient broker2 = createBroker();
        TestClient broker3 = createBroker();
    
        // Then: All receive unique IDs starting with 1
        assertThat(broker1.getClientId()).isEqualTo("100001");
        assertThat(broker2.getClientId()).isEqualTo("100002");
        assertThat(broker3.getClientId()).isEqualTo("100003");
    
        logger.info("✓ 3 brokers received sequential 6-digit IDs");
    
        broker1.close();
        broker2.close();
        broker3.close();
    }

    @Test
    @DisplayName("Multiple markets should receive unique 6-digit IDs")
    public void testMultipleMarkets() throws Exception {
        // When: 2 markets connect
        TestClient market1 = createMarket();
        TestClient market2 = createMarket();
    
        // Then: All receive unique IDs starting with 2
        assertThat(market1.getClientId()).isEqualTo("200001");
        assertThat(market2.getClientId()).isEqualTo("200002");
    
        logger.info("✓ 2 markets received sequential 6-digit IDs");
    
        market1.close();
        market2.close();
    }

    @Test
    @DisplayName("Broker and Market IDs should have different prefixes")
    public void testIndependentIds() throws Exception {
        // When: Broker and Market connect
        TestClient broker = createBroker();
        TestClient market = createMarket();
    
        // Then: IDs start with different digits
        assertThat(broker.getClientId()).isEqualTo("100001");
        assertThat(market.getClientId()).isEqualTo("200001");
    
        logger.info("✓ Broker (1xxxxx) and Market (2xxxxx) have distinct 6-digit ID ranges");
    
        broker.close();
        market.close();
    }
}