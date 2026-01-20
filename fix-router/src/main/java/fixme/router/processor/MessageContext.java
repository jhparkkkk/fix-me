package fixme.router.processor;

import fixme.common.message.FixMessage;
import fixme.router.nio.ClientConnection;

/**
 * Holds context information for processing a FIX message.
 * Includes the raw message, parsed FIX message, source connection,
 * target connection, and validation status.
*/

public class MessageContext {
    private final String rawMessage;
    private final ClientConnection source;

    private FixMessage fixMessage;
    private ClientConnection target;
    private boolean valid;
    private String errorMessage;

    public MessageContext(String rawMessage, ClientConnection source) {
        this.rawMessage = rawMessage;
        this.source = source;
        this.valid = true;
    }

    public String getRawMessage() {
        return rawMessage;
    }

    public ClientConnection getSource() {
        return source;
    }

    public FixMessage getFixMessage() {
        return fixMessage;
    }

    public ClientConnection getTarget() {
        return target;
    }

    public boolean isValid() {
        return valid;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setFixMessage(FixMessage fixMessage) {
        this.fixMessage = fixMessage;
    }

    public void setTarget(ClientConnection target) {
        this.target = target;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        this.valid = false;
    }

    public void fail(String error) {
        this.valid = false;
        this.errorMessage = error;
    }

    @Override
    public String toString() {
        return String.format("MessageContext[source=%s, target=%s, valid=%s, error=%s]",
            source != null ? source.getClientId() : "null",
            target != null ? target.getClientId() : "null",
            valid,
            errorMessage
        );
    }
}