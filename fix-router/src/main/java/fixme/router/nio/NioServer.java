package fixme.router.nio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fixme.router.ComponentType;
import fixme.router.connection.ConnectionManager;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Non-blocking NIO Router server that accepts connections on multiple ports.
 * Implements the Reactor pattern with a single-threaded event loop.
 * 
 * The Router listens on:
 * - Port 5000 for Broker connections
 * - Port 5001 for Market connections
 * 
 * This single server manages both types of connections with ONE Selector.
 * 
 * Design Pattern: Reactor
 */
public class NioServer implements Runnable {
    
    private static final Logger logger = LoggerFactory.getLogger(NioServer.class);
    private static final int SELECT_TIMEOUT = 1000; // 1 second
    
    private final int brokerPort;
    private final int marketPort;
    private final ConnectionManager connectionManager;
    private final MessageDispatcher messageDispatcher;
    
    private Selector selector;
    private ServerSocketChannel brokerServerChannel;
    private ServerSocketChannel marketServerChannel;
    
    // Map to identify component type by ServerSocketChannel
    private final Map<ServerSocketChannel, ComponentType> channelTypeMap;
    
    private volatile boolean running;
    
    /**
     * Create a Router server that listens on two ports.
     * 
     * @param brokerPort Port for Broker connections (5000)
     * @param marketPort Port for Market connections (5001)
     * @param connectionManager Manager for all connections
     * @param messageDispatcher Dispatcher for received messages
     */
    public NioServer(int brokerPort, int marketPort,
                     ConnectionManager connectionManager,
                     MessageDispatcher messageDispatcher) {
        this.brokerPort = brokerPort;
        this.marketPort = marketPort;
        this.connectionManager = connectionManager;
        this.messageDispatcher = messageDispatcher;
        this.channelTypeMap = new HashMap<>();
        this.running = false;
    }
    
    /**
     * Initialize the server: create selector and server socket channels for both ports.
     * 
     * @throws IOException if initialization fails
     */
    public void initialize() throws IOException {
        logger.info("Initializing Router server...");
        logger.info("  - Broker port: {}", brokerPort);
        logger.info("  - Market port: {}", marketPort);
        
        selector = Selector.open();
        
        brokerServerChannel = ServerSocketChannel.open();
        brokerServerChannel.configureBlocking(false);
        brokerServerChannel.socket().bind(new InetSocketAddress(brokerPort));
        brokerServerChannel.register(selector, SelectionKey.OP_ACCEPT);
        channelTypeMap.put(brokerServerChannel, ComponentType.BROKER);
        logger.info("Broker server channel bound to port {}", brokerPort);
        
        marketServerChannel = ServerSocketChannel.open();
        marketServerChannel.configureBlocking(false);
        marketServerChannel.socket().bind(new InetSocketAddress(marketPort));
        marketServerChannel.register(selector, SelectionKey.OP_ACCEPT);
        channelTypeMap.put(marketServerChannel, ComponentType.MARKET);
        logger.info("Market server channel bound to port {}", marketPort);
        
        logger.info("Router server initialized successfully");
        logger.info("  - Using single Selector for both ports");
    }
    
    @Override
    public void run() {
        running = true;
        logger.info("Router server started - listening on ports {} and {}", 
                   brokerPort, marketPort);
        
        try {
            while (running) {
                int readyChannels = selector.select(SELECT_TIMEOUT);
                
                if (readyChannels == 0) {
                    continue;
                }
                
                // Process selected keys (from both Broker and Market ports)
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> keyIterator = selectedKeys.iterator();
                
                while (keyIterator.hasNext()) {
                    SelectionKey key = keyIterator.next();
                    keyIterator.remove();
                    
                    if (!key.isValid()) {
                        continue;
                    }
                    
                    try {
                        if (key.isAcceptable()) {
                            handleAccept(key);
                        } else if (key.isReadable()) {
                            handleRead(key);
                        } else if (key.isWritable()) {
                            handleWrite(key);
                        }
                    } catch (Exception e) {
                        logger.error("Error handling key event", e);
                        handleError(key);
                    }
                }
            }
        } catch (IOException e) {
            logger.error("Error in server event loop", e);
        } finally {
            cleanup();
        }
        
        logger.info("Router server stopped");
    }
    
    private void handleAccept(SelectionKey key) throws IOException {
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel clientChannel = serverChannel.accept();
        
        if (clientChannel == null) {
            return;
        }
        
        ComponentType componentType = channelTypeMap.get(serverChannel);
        
        logger.info("New {} connection from {}", 
                   componentType, clientChannel.getRemoteAddress());
        
        clientChannel.configureBlocking(false);
        
        ClientConnection connection = connectionManager.registerConnection(
            clientChannel, componentType
        );
        
        SelectionKey clientKey = clientChannel.register(selector, SelectionKey.OP_READ);
        clientKey.attach(connection);
        
        sendIdToClient(connection);
        
        logger.info("Assigned ID {} to new {} connection", 
                   connection.getClientId(), componentType);
    }
    
    private void sendIdToClient(ClientConnection connection) {
        String idMessage = "ID=" + connection.getClientId() + "|";
        connection.queueMessage(idMessage);
        connection.setIdentified(true);
        try {
            SelectionKey key = connection.getChannel().keyFor(selector);
            if (key != null && key.isValid()) {
                key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
            }
        } catch (Exception e) {
            logger.error("Error registering write interest for ID message", e);
        }
    }
    
    private void handleRead(SelectionKey key) throws IOException {
        ClientConnection connection = (ClientConnection) key.attachment();
        
        String data = connection.read();
        
        if (data == null) {
            handleDisconnect(key, connection);
            return;
        }
        
        if (data.isEmpty()) {
            // No complete message yet
            return;
        }
        
        logger.debug("Received data from {}: {} chars", 
                    connection.getClientId(), data.length());
        
        if (messageDispatcher != null) {
            messageDispatcher.dispatch(data, connection);
        }
    }
    
    private void handleWrite(SelectionKey key) throws IOException {
        ClientConnection connection = (ClientConnection) key.attachment();
        
        connection.write();
        
        // If no more data to write, remove WRITE interest
        if (!connection.hasDataToWrite()) {
            key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
        }
    }
    
    private void handleDisconnect(SelectionKey key, ClientConnection connection) {
        logger.info("{} disconnected: {}", connection.getType(), connection.getClientId());
        
        key.cancel();
        connectionManager.unregisterConnection(connection.getClientId());
        connection.close();
    }
    
    private void handleError(SelectionKey key) {
        ClientConnection connection = (ClientConnection) key.attachment();
        
        if (connection != null) {
            handleDisconnect(key, connection);
        } else {
            key.cancel();
        }
    }
    
    public void stop() {
        logger.info("Stopping Router server...");
        running = false;
        
        if (selector != null && selector.isOpen()) {
            selector.wakeup();
        }
    }
    
    private void cleanup() {
        logger.info("Cleaning up Router server resources");
        
        try {
            if (brokerServerChannel != null && brokerServerChannel.isOpen()) {
                brokerServerChannel.close();
                logger.info("Closed Broker server channel");
            }
            
            if (marketServerChannel != null && marketServerChannel.isOpen()) {
                marketServerChannel.close();
                logger.info("Closed Market server channel");
            }
            
            if (selector != null && selector.isOpen()) {
                for (SelectionKey key : selector.keys()) {
                    try {
                        key.channel().close();
                    } catch (IOException e) {
                        logger.warn("Error closing channel", e);
                    }
                }
                selector.close();
                logger.info("Closed selector");
            }
        } catch (IOException e) {
            logger.error("Error during cleanup", e);
        }
    }
    
    public boolean isRunning() {
        return running;
    }
    
    public int getBrokerPort() {
        return brokerPort;
    }
    
    public int getMarketPort() {
        return marketPort;
    }
}