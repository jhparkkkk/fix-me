package fixme.router.nio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fixme.router.ComponentType;


/**
 * Represents an active client connection to the router.
 * Encapsulates all connection state including buffers and queues.
 */
public class ClientConnection {

    private static final Logger logger = LoggerFactory.getLogger(ClientConnection.class);

    private static final int BUFFER_SIZE = 8192;

    private final String clientId;
    private final SocketChannel channel;
    private final ComponentType type;
    private final Instant connectedAt;
    
    private final ByteBuffer readBuffer;
    private final ConcurrentLinkedQueue<ByteBuffer> writeQueue;

    // message accumulation buffer for partial reads
    private final StringBuilder messageBuilder;

    private volatile boolean identified;

    public ClientConnection(String clientId, SocketChannel channel, ComponentType componentType) {
        this.clientId = clientId;
        this.channel = channel;
        this.type = componentType;
        this.connectedAt = Instant.now();
        this.readBuffer = ByteBuffer.allocate(BUFFER_SIZE);
        this.writeQueue = new ConcurrentLinkedQueue<>();
        this.messageBuilder = new StringBuilder();
        this.identified = false;

        logger.info("New client connection established: {} of type {}", clientId, componentType);
    }

    public String getClientId() {
        return clientId;
    }

    public SocketChannel getChannel() {
        return channel;
    }

    public ComponentType getType() {
        return type;
    }

    public Instant getConnectedAt() {
        return connectedAt;
    }

    public boolean isIdentified() {
        return identified;
    }

    public void setIdentified(boolean identified) {
        this.identified = identified;
    }

    public void queueMessage(String message) {
        if (message == null || message.isEmpty()) {
            logger.warn("Attempted to queue null or empty message for client {}", clientId);
            return;
        }

        byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        writeQueue.offer(buffer);

        logger.debug("Queued message for client {}: {}", clientId, message);

    }

    public boolean hasDataToWrite() {
        return !writeQueue.isEmpty();
    }

    /**
     * Writes pending messages to the client channel.
     * Non-blocking; may perform partial writes.
     * @throws IOException
     */
    public void write() throws IOException {

        ByteBuffer buffer = writeQueue.peek();
        while (buffer != null) {
            int written = channel.write(buffer);
            if (written > 0) {
                logger.debug("Wrote {} bytes to client {}", written, clientId);
            }

            if (!buffer.hasRemaining()) {
                writeQueue.poll();
                buffer = writeQueue.peek();
                logger.debug("Completed writing message to client {}", clientId);
            } else {
                break;
            }
        }
    }

    public String read() throws IOException {

        readBuffer.clear();
        
        int bytesRead = channel.read(readBuffer);
        
        if (bytesRead == -1) {
            logger.info("Client {} disconnected", clientId);
            return null;
        }
        
        if (bytesRead == 0) {
            return ""; // No data read
        }

        readBuffer.flip();
        String data = StandardCharsets.UTF_8.decode(readBuffer).toString();
        messageBuilder.append(data);

        String accumulated = messageBuilder.toString();

        if (accumulated.contains("|")) {
            int lastDelimiter = accumulated.lastIndexOf('|');
            String completeMessages = accumulated.substring(0, lastDelimiter + 1);
            String remainder = accumulated.substring(lastDelimiter + 1);
            
            messageBuilder.setLength(0);
            messageBuilder.append(remainder);
            
            logger.debug("Extracted complete message(s) from {}", clientId);
            
            return completeMessages;
        }
        return "";
    }

    public void close() {
        try {
            channel.close();
            logger.info("Closed connection for client {}", clientId);
        } catch (IOException e) {
            logger.error("Error closing connection for client {}", clientId, e);
        }
    }

    @Override
    public String toString() {
        return String.format("ClientConnection{id=%s, type=%s, connectedAt=%s}", 
                clientId, type, connectedAt);
    }
}                                                                               
