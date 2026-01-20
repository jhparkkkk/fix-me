package fixme.router.processor.handler;

import fixme.router.processor.MessageContext;


public interface MessageHandler {
    
    boolean handle(MessageContext context);

    default String getName() {
        return this.getClass().getSimpleName();
    }
    
}
