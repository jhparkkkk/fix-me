package fixme.router;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fixme.common.config.FixConfig;
import fixme.router.connection.ConnectionManager;
import fixme.router.nio.MessageDispatcher;
import fixme.router.nio.NioServer;
import fixme.router.processor.MessageProcessor;
import fixme.router.routing.RoutingTable;

import java.io.IOException;

/**
 * Main application for the FIX Router.
 * 
 * The Router is the central component that:
 * - Accepts connections from Brokers (port 5000) and Markets (port 5001)
 * - Assigns unique 6-digit IDs to each connected component
 * - Validates incoming messages based on checksum
 * - Routes messages to their destination using the routing table
 * - Forwards messages to the destination
 * - Performs NO business logic, only message forwarding
 * 
 * The Router acts as a Market Connectivity Provider, allowing Brokers
 * to send messages to Markets without depending on specific Market implementations.
 * 
 * Phase 1: Basic infrastructure - accepts connections and assigns IDs ✅
 * Phase 2: Message routing and validation ✅
 */
public class RouterApplication {
    
    private static final Logger logger = LoggerFactory.getLogger(RouterApplication.class);
    
    // Port configuration as per specification
    private static final int BROKER_PORT = 5000;  // Port for Broker connections
    private static final int MARKET_PORT = 5001;  // Port for Market connections
    
    private static final int MESSAGE_PROCESSOR_THREADS = 4; // Thread pool size
    
    public static void main(String[] args) {
        logger.info("=".repeat(60));
        logger.info("Starting FIX Router...");
        logger.info("=".repeat(60));
        
        try {
            // Load FIX configuration
            FixConfig config = FixConfig.getInstance();
            logger.info("Loaded FIX configuration: {} v{}", 
                       config.getProtocol(), config.getVersion());
            
            // Create Phase 2 components
            RoutingTable routingTable = new RoutingTable();
            ConnectionManager connectionManager = new ConnectionManager(routingTable);
            MessageProcessor messageProcessor = new MessageProcessor(
                routingTable, 
                MESSAGE_PROCESSOR_THREADS
            );
            MessageDispatcher messageDispatcher = new MessageDispatcher(
                config.getDelimiter(),
                messageProcessor
            );
            
            // Create THE Router server that listens on BOTH ports
            NioServer routerServer = new NioServer(
                BROKER_PORT,
                MARKET_PORT,
                connectionManager,
                messageDispatcher
            );
            
            // Initialize the server (binds both ports)
            routerServer.initialize();
            
            // Start the server in its own thread
            Thread serverThread = new Thread(routerServer, "RouterServer");
            serverThread.start();
            
            logger.info("=".repeat(60));
            logger.info("FIX Router started successfully");
            logger.info("  - Single server listening on 2 ports:");
            logger.info("    * Port {} for Brokers (assigns IDs: 100001, 100002...)", BROKER_PORT);
            logger.info("    * Port {} for Markets (assigns IDs: 200001, 200002...)", MARKET_PORT);
            logger.info("");
            logger.info("Press Ctrl+C to stop...");
            logger.info("=".repeat(60));
            
            // Add shutdown hook for graceful shutdown
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("");
                logger.info("=".repeat(60));
                logger.info("Shutdown signal received");
                logger.info("=".repeat(60));
                
                // Stop NioServer
                routerServer.stop();
                
                // Shutdown MessageProcessor
                messageProcessor.shutdown();
                
                // Wait for server thread
                try {
                    serverThread.join(5000);
                } catch (InterruptedException e) {
                    logger.warn("Interrupted while waiting for server to stop", e);
                }
                
                logger.info("FIX Router stopped");
                logger.info("=".repeat(60));
            }));
            
            // Wait for server thread to finish
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