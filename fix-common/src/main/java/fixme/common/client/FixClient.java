package fixme.common.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Abstract base class for FIX protocol clients (Broker and Market).
 * Handles the common socket connection logic.
 */
public abstract class FixClient {
    
    private static final Logger logger = LoggerFactory.getLogger(FixClient.class);
    
    private static final String ROUTER_HOST = "localhost";
    
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private String clientId;
    
    protected abstract int getRouterPort();
    
    protected abstract String getClientType();
    
    public void connect() throws IOException {
        int port = getRouterPort();
        
        logger.info("Connecting to router at {}:{}", ROUTER_HOST, port);
        
        socket = new Socket(ROUTER_HOST, port);
        reader = new BufferedReader(
            new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8)
        );
        writer = new PrintWriter(
            new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8),
            true
        );
        
        logger.info("Connected, waiting for ID...");
        
        String idMessage = reader.readLine();
        
        if (idMessage != null && idMessage.startsWith("ID=") && idMessage.contains("|")) {
            int startIdx = 3;
            int endIdx = idMessage.indexOf('|');
            clientId = idMessage.substring(startIdx, endIdx);
            logger.info("✓ Received {} ID: {}", getClientType(), clientId);
        } else {
            throw new IOException("Failed to receive " + getClientType() + " ID. Got: " + idMessage);
        }
    }

    public void sendMessage(String message) throws IOException {
        if (writer == null || socket.isClosed()) {
            throw new IOException("Not connected");
        }
        
        logger.debug("Sending: {}", message);
        writer.println(message);
    }
    
    public String receiveMessage() throws IOException {
        if (reader == null || socket.isClosed()) {
            throw new IOException("Not connected");
        }
        
        String message = reader.readLine();
        if (message != null) {
            logger.debug("Received: {}", message);
        }
        
        return message;
    }
    
    public void close() {
        try {
            if (reader != null) reader.close();
            if (writer != null) writer.close();
            if (socket != null) socket.close();
            logger.info("Disconnected from router");
        } catch (IOException e) {
            logger.warn("Error closing connection", e);
        }
    }
    
    public String getClientId() {
        return clientId;
    }
    
    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }
}