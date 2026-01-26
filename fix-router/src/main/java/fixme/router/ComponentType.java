package fixme.router;

/**
 * Types of components connected to the router.
 */

public enum ComponentType {
    BROKER("1", 5000),
    MARKET("2", 5001);

    private final String prefix;
    private final int port;

    ComponentType(String prefix, int port) {
        this.prefix = prefix;
        this.port = port;
    }

    public String getPrefix() {
        return prefix;
    }

    public int getPort() {
        return port;
    }

    @Override
    public String toString() {
        return name() + "(port " + port + ")";
    }
}