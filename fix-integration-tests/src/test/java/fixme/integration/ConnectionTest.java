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
    @DisplayName("Broker should connect and receive ID starting with B")
    public void testBrokerConnection() throws Exception {
        // Given: Router is running
        
        // When: Broker connects
        TestClient broker = createBroker();
        
        // Then: Broker receives ID
        assertThat(broker.getClientId())
            .isNotNull()
            .startsWith("B")
            .hasSize(6);
        
        logger.info("✓ Broker received valid ID: {}", broker.getClientId());
        
        broker.close();
    }
    
    @Test
    @DisplayName("Market should connect and receive ID starting with M")
    public void testMarketConnection() throws Exception {
        // Given: Router is running
        
        // When: Market connects
        TestClient market = createMarket();
        
        // Then: Market receives ID
        assertThat(market.getClientId())
            .isNotNull()
            .startsWith("M")
            .hasSize(6);
        
        logger.info("✓ Market received valid ID: {}", market.getClientId());
        
        market.close();
    }
    
    @Test
    @DisplayName("Multiple brokers should receive unique IDs")
    public void testMultipleBrokers() throws Exception {
        // Given: Router is running
        
        // When: 3 brokers connect
        TestClient broker1 = createBroker();
        TestClient broker2 = createBroker();
        TestClient broker3 = createBroker();
        
        // Then: All receive unique IDs
        assertThat(broker1.getClientId()).isEqualTo("B00001");
        assertThat(broker2.getClientId()).isEqualTo("B00002");
        assertThat(broker3.getClientId()).isEqualTo("B00003");
        
        logger.info("✓ 3 brokers received sequential IDs");
        
        broker1.close();
        broker2.close();
        broker3.close();
    }
    
    @Test
    @DisplayName("Multiple markets should receive unique IDs")
    public void testMultipleMarkets() throws Exception {
        // Given: Router is running
        
        // When: 2 markets connect
        TestClient market1 = createMarket();
        TestClient market2 = createMarket();
        
        // Then: All receive unique IDs
        assertThat(market1.getClientId()).isEqualTo("M00001");
        assertThat(market2.getClientId()).isEqualTo("M00002");
        
        logger.info("✓ 2 markets received sequential IDs");
        
        market1.close();
        market2.close();
    }
    
    @Test
    @DisplayName("Broker and Market IDs should be independent")
    public void testIndependentIds() throws Exception {
        // Given: Router is running
        
        // When: Broker and Market connect
        TestClient broker = createBroker();
        TestClient market = createMarket();
        
        // Then: IDs are independent
        assertThat(broker.getClientId()).isEqualTo("B00001");
        assertThat(market.getClientId()).isEqualTo("M00001");
        
        logger.info("✓ Broker and Market have independent ID sequences");
        
        broker.close();
        market.close();
    }
}