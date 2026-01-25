package fixme.integration;

import fixme.common.client.FixClient;
import fixme.router.nio.NioServer;
import fixme.router.connection.ConnectionManager;
import fixme.router.nio.MessageDispatcher;
import fixme.router.processor.MessageProcessor;
import fixme.router.routing.RoutingTable;
import fixme.common.config.FixConfig;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Base class for integration tests.
 * Manages Router startup/shutdown and provides test utilities.
 */
public abstract class IntegrationTestBase {
    
    protected static final Logger logger = LoggerFactory.getLogger(IntegrationTestBase.class);
    
    protected static final int BROKER_PORT = 5000;
    protected static final int MARKET_PORT = 5001;
    protected static final String HOST = "localhost";
    
    protected Thread routerThread;
    protected NioServer routerServer;
    
    @BeforeEach
    public void setupRouter() throws Exception {
        logger.info("=".repeat(60));
        logger.info("Starting Router for integration test");
        logger.info("=".repeat(60));
        
        // Load configuration
        FixConfig config = FixConfig.getInstance();
        
        // Create router components
        RoutingTable routingTable = new RoutingTable();
        ConnectionManager connectionManager = new ConnectionManager(routingTable);
        MessageProcessor messageProcessor = new MessageProcessor(routingTable, 2);
        MessageDispatcher messageDispatcher = new MessageDispatcher(
            config.getDelimiter(),
            messageProcessor
        );
        
        // Create and initialize router
        routerServer = new NioServer(
            BROKER_PORT,
            MARKET_PORT,
            connectionManager,
            messageDispatcher
        );
        
        routerServer.initialize();
        
        // Start router in separate thread
        routerThread = new Thread(routerServer, "TestRouter");
        routerThread.start();
        
        // Wait for router to be ready
        Thread.sleep(500);
        
        logger.info("Router started successfully");
    }
    
    @AfterEach
    public void teardownRouter() throws Exception {
        logger.info("Stopping Router");
        
        if (routerServer != null) {
            routerServer.stop();
        }
        
        if (routerThread != null) {
            routerThread.join(2000);
        }
        
        // Give time for cleanup
        Thread.sleep(200);
        
        logger.info("Router stopped");
    }
    
    /**
     * Creates a test broker client connection.
     */
    protected TestClient createBroker() throws IOException {
        return new TestClient(BROKER_PORT, "Broker");
    }
    
    /**
     * Creates a test market client connection.
     */
    protected TestClient createMarket() throws IOException {
        return new TestClient(MARKET_PORT, "Market");
    }
    
    /**
     * Test client wrapper for integration tests.
     */
    protected static class TestClient {
        private final Socket socket;
        private final BufferedReader reader;
        private final PrintWriter writer;
        private String clientId;
        private final String type;
        
        public TestClient(int port, String type) throws IOException {
            this.type = type;
            this.socket = new Socket(HOST, port);
            this.reader = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8)
            );
            this.writer = new PrintWriter(
                new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8),
                true
            );
            
            // Receive ID
            String idMessage = reader.readLine();
            if (idMessage != null && idMessage.startsWith("ID=")) {
                this.clientId = idMessage.substring(3, idMessage.indexOf('|'));
                logger.info("{} connected with ID: {}", type, clientId);
            } else {
                throw new IOException("Failed to receive ID: " + idMessage);
            }
        }
        
        public String getClientId() {
            return clientId;
        }
        
        public void send(String message) {
            logger.debug("{} sending: {}", type, message);
            writer.println(message);
        }
        
        public String receive() throws IOException {
            return receive(10000); // 10 second timeout (increased for high load)
        }
        
        public String receive(long timeoutMs) throws IOException {
            socket.setSoTimeout((int) timeoutMs);
            String message = reader.readLine();
            logger.debug("{} received: {}", type, message);
            return message;
        }
        
        public void close() throws IOException {
            reader.close();
            writer.close();
            socket.close();
            logger.info("{} disconnected", type);
        }
    }
    
    /**
     * Strips forwarding prefix from router messages.
     */
    protected String stripPrefix(String message) {
        if (message != null && message.startsWith("[")) {
            int idx = message.indexOf(']');
            if (idx > 0) {
                return message.substring(idx + 1).trim();
            }
        }
        return message;
    }
    
    /**
     * Waits for a condition to be true.
     */
    protected void waitFor(int maxWaitMs, BooleanSupplier condition) throws InterruptedException {
        long start = System.currentTimeMillis();
        while (!condition.getAsBoolean()) {
            if (System.currentTimeMillis() - start > maxWaitMs) {
                throw new AssertionError("Timeout waiting for condition");
            }
            Thread.sleep(100);
        }
    }
    
    @FunctionalInterface
    protected interface BooleanSupplier {
        boolean getAsBoolean();
    }
}