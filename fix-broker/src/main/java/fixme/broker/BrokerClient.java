package fixme.broker;

import fixme.common.client.FixClient;

/**
 * Broker client that connects to router on port 5000.
 */
public class BrokerClient extends FixClient {
    
    private static final int BROKER_PORT = 5000;
    
    @Override
    protected int getRouterPort() {
        return BROKER_PORT;
    }
    
    @Override
    protected String getClientType() {
        return "Broker";
    }
    
    public String getBrokerId() {
        return getClientId();
    }
}