package fixme.broker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class BrokerClient {
    
    private static final Logger logger = LoggerFactory.getLogger(BrokerClient.class);

    private static final String ROUTER_HOST = "localhost";
    private static final int ROUTER_PORT = 5000;

    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private String brokerId;

    public void connect() throws IOException {
        logger.info("Connecting to router at {}:{}", ROUTER_HOST, ROUTER_PORT);
        socket = new Socket(ROUTER_HOST, ROUTER_PORT);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
        logger.info("Connected to router at {}:{}", ROUTER_HOST, ROUTER_PORT);

        String idMessage = reader.readLine();
        if (idMessage != null && idMessage.startsWith("ID=")) {
            int startIdx = idMessage.indexOf("ID=") + 3;
            int endIdx = idMessage.indexOf("|");
            brokerId = idMessage.substring(startIdx, endIdx);
            logger.info("Received Broker ID: {}", brokerId);
        } else {
            throw new IOException("Failed to receive Broker ID from router");
        }

    }

    public void sendMessage(String message) throws IOException {
        if (writer == null || socket.isClosed()) {
            throw new IOException("Not connected to router");
        }
        writer.println(message);
        logger.info("Sent message: {}", message);
    }

    public String receiveMessage() throws IOException {
        if (reader == null || socket.isClosed()) {
            throw new IOException("Not connected to router");
        }
        String message = reader.readLine();
        if (message != null) {
            logger.info("Received message: {}", message);
        }
        return message;
    }

    public void close() {
        try {
            if (reader != null) {
                reader.close();
            }
            if (writer != null) {
                writer.close();
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            logger.info("Connection to router closed");
        } catch (IOException e) {
            logger.error("Error closing connection: {}", e.getMessage());
        }
    }

    public String getBrokerId() {
        return brokerId;
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected();
    }

}
