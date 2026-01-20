package fixme.router;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fixme.common.config.FixConfig;
import fixme.router.connection.ConnectionManager;
import fixme.router.nio.MessageDispatcher;
import fixme.router.nio.NioServer;

import java.io.IOException;

/**
 * Main application for the FIX Router.
 * 
 * The Router is the central component that:
 * - Accepts connections from Brokers (port 5000) and Markets (port 5001)
 * - Assigns unique 6-digit IDs to each connected component
 * - Validates incoming messages based on checksum
 * - Routes messages to their destination using the routing table
 * - Performs NO business logic, only message forwarding
 * 
 */
public class RouterApplication {
    
    private static final Logger logger = LoggerFactory.getLogger(RouterApplication.class);
    
    private static final int BROKER_PORT = 5000;  
    private static final int MARKET_PORT = 5001;
    
    public static void main(String[] args) {
        logger.info("=".repeat(60));
        logger.info("Starting FIX Router...");
        logger.info("=".repeat(60));
        
        try {
            FixConfig config = FixConfig.getInstance();
            logger.info("Loaded FIX configuration: {} v{}", 
                       config.getProtocol(), config.getVersion());
            
            ConnectionManager connectionManager = new ConnectionManager();
            MessageDispatcher messageDispatcher = new MessageDispatcher(config.getDelimiter());
            
            NioServer routerServer = new NioServer(
                BROKER_PORT,
                MARKET_PORT,
                connectionManager,
                messageDispatcher
            );
            
            routerServer.initialize();
            
            Thread serverThread = new Thread(routerServer, "RouterServer");
            serverThread.start();
            
            logger.info("=".repeat(60));
            logger.info("FIX Router started successfully");
            logger.info("  - Single server listening on 2 ports:");
            logger.info("    * Port {} for Brokers (assigns IDs: B00001, B00002...)", BROKER_PORT);
            logger.info("    * Port {} for Markets (assigns IDs: M00001, M00002...)", MARKET_PORT);
            logger.info("");
            logger.info("Architecture:");
            logger.info("  - 1 NioServer (Reactor pattern)");
            logger.info("  - 1 Selector (handles both ports)");
            logger.info("  - 1 Thread (event loop)");
            logger.info("");
            logger.info("Router responsibilities:");
            logger.info("  1. Accept connections and assign unique 6-digit IDs");
            logger.info("  2. Validate messages based on checksum");
            logger.info("  3. Route messages to destination using routing table");
            logger.info("");
            logger.info("Press Ctrl+C to stop...");
            logger.info("=".repeat(60));
            
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("");
                logger.info("=".repeat(60));
                logger.info("Shutdown signal received");
                logger.info("=".repeat(60));
                
                routerServer.stop();
                
                try {
                    serverThread.join(5000);
                } catch (InterruptedException e) {
                    logger.warn("Interrupted while waiting for server to stop", e);
                }
                
                logger.info("FIX Router stopped");
                logger.info("=".repeat(60));
            }));
            
            serverThread.join();
            
        } catch (IOException e) {
            logger.error("Failed to start router", e);
            System.exit(1);
        } catch (InterruptedException e) {
            logger.error("Router interrupted", e);
            Thread.currentThread().interrupt();
        }
    }
}