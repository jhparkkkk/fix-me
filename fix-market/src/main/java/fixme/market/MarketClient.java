package fixme.market;

import fixme.common.client.FixClient;

/**
 * Market client that connects to router on port 5001.
 */
public class MarketClient extends FixClient {
    
    private static final int MARKET_PORT = 5001;
    
    @Override
    protected int getRouterPort() {
        return MARKET_PORT;
    }
    
    @Override
    protected String getClientType() {
        return "Market";
    }
    
    public String getMarketId() {
        return getClientId();
    }
}