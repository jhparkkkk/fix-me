package fixme.router.nio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

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
    private Selector selector;
    
    private final ByteBuffer readBuffer;
    private final ConcurrentLinkedQueue<ByteBuffer> writeQueue;

    // message accumulation buffer for partial reads
    private final StringBuilder messageBuilder;

    private volatile boolean identified;
    private volatile boolean markedForClosure = false;

    private static final int MAX_CONSECUTIVE_ERRORS = 5;
    private final AtomicInteger consecutiveErrorCount = new AtomicInteger(0);


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

    public void setSelector(Selector selector) {
        this.selector = selector; 
    }

    public void enableWriteInterest() {
        if (selector == null) {
            logger.warn("Cannot enable write interest - selector not set for {}", clientId);
            return;
        }
        
        try {
            SelectionKey key = channel.keyFor(selector);
            if (key != null && key.isValid()) {
                key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
                logger.debug("Enabled OP_WRITE for {}", clientId);
            }
        } catch (Exception e) {
            logger.error("Error enabling write interest for {}", clientId, e);
        }
    }

    public void queueMessage(String message) {
        if (message == null || message.isEmpty()) {
            logger.warn("Attempted to queue null or empty message for client {}", clientId);
            return;
        }

        String messageToSend = message.endsWith("\n") ? message : message + "\n";
        byte[] bytes = messageToSend.getBytes(StandardCharsets.UTF_8);
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        writeQueue.offer(buffer);

        logger.debug("Queued message for client {}: {}", clientId, messageToSend);
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

    public void markForClosure() {
        this.markedForClosure = true;
        logger.info("Marked connection for client {} for closure", clientId);
    }

    public boolean shouldClose() {
        return markedForClosure && !hasDataToWrite();
    }

    public boolean incrementErrorCount() {
        int errorsCount = consecutiveErrorCount.incrementAndGet();
        logger.debug("Error count for client {}: {}/{}", clientId, errorsCount, MAX_CONSECUTIVE_ERRORS);
        return errorsCount >= MAX_CONSECUTIVE_ERRORS;
    }

    public void resetErrorCount() {
        int previousErrorCount = consecutiveErrorCount.getAndSet(0);
        if (previousErrorCount > 0) {
            logger.debug("Reset error count for client {} from {}", clientId, previousErrorCount);
        }
    }

    public int getErrorCount() {
        return consecutiveErrorCount.get();
    }



    @Override
    public String toString() {
        return String.format("ClientConnection{id=%s, type=%s, connectedAt=%s}", 
                clientId, type, connectedAt);
    }
}                                                                               
