package fixme.router.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fixme.router.nio.ClientConnection;
import fixme.router.processor.handler.MessageHandler;
import fixme.router.processor.handler.ValidationHandler;
import fixme.router.processor.handler.RoutingHandler;
import fixme.router.processor.handler.ForwardingHandler;
import fixme.router.routing.RoutingTable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;


public class MessageProcessor {
    private static final Logger logger = LoggerFactory.getLogger(MessageProcessor.class);
    private static final int THREAD_POOL_SIZE = 4;

    private final ExecutorService executorService;
    private final List<MessageHandler> handlerChain;

    public MessageProcessor(RoutingTable routingTable) {
        this(routingTable, THREAD_POOL_SIZE);
    }

    public MessageProcessor(RoutingTable routingTable, int threadPoolSize) {
        this.executorService = Executors.newFixedThreadPool(threadPoolSize, new MessageProcessorThreadFactory());

        this.handlerChain = new ArrayList<>();
        this.handlerChain.add(new ValidationHandler());
        this.handlerChain.add(new RoutingHandler(routingTable));
        this.handlerChain.add(new ForwardingHandler());
        
        logger.info("Initialized MessageProcessor with {} threads and {} handlers", threadPoolSize, handlerChain.size());
    }

    public void processMessage(String rawMessage, ClientConnection source){
        logger.debug("Submitting message from {} for processing", source.getClientId());
        executorService.submit(() -> {
            processMessageSync(rawMessage, source);
        });
    }

    private void processMessageSync(String rawMessage, ClientConnection source) {
        logger.debug("[{}] Processing message from {}", 
                    Thread.currentThread().getName(), 
                    source.getClientId());
        
        MessageContext context = new MessageContext(rawMessage, source);
        
        try {
            for (MessageHandler handler : handlerChain) {
                logger.debug("[{}] Executing handler: {}", 
                           Thread.currentThread().getName(),
                           handler.getName());
                
                boolean continueProcessing = handler.handle(context);
                
                if (!continueProcessing) {
                    logger.warn("Handler {} stopped processing: {}", 
                               handler.getName(), 
                               context.getErrorMessage());
                    break;
                }
            }
            
            if (context.isValid()) {
                logger.info("[{}] Message processed successfully: {} → {}", 
                           Thread.currentThread().getName(),
                           source.getClientId(),
                           context.getTarget().getClientId());
            } else {
                logger.warn("[{}] Message processing failed: {}", 
                           Thread.currentThread().getName(),
                           context.getErrorMessage());
            }
            
        } catch (Exception e) {
            logger.error("Error processing message from {}: {}", 
                        source.getClientId(), e.getMessage(), e);
        }
    }

    public void shutdown() {
        logger.info("Shutting down MessageProcessor...");
        executorService.shutdown();
        logger.info("MessageProcessor shut down complete.");
    }

    private static class MessageProcessorThreadFactory implements ThreadFactory {
        private final AtomicInteger threadNumber = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, "MessageProcessor-" + threadNumber.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        }
    }


}
