package fixme.common.message;

public final class FixTags {
    
    // System tags
    public static final String SENDER_COMP_ID = "49";
    public static final String TARGET_COMP_ID = "56";
    public static final String MSG_TYPE = "35";
    public static final String CHECKSUM = "10";

    // Order tags
    public static final String SYMBOL= "55";
    public static final String SIDE = "54";
    public static final String ORDER_QTY = "38";
    public static final String PRICE = "44";

    // Execution tags
    public static final String ORD_STATUS = "39";
    public static final String TEXT = "58";

    // Message Types
    public static final String MSG_TYPE_NEW_ORDER = "D";
    public static final String MSG_TYPE_EXECUTION_REPORT = "8";

    // Side values
    public static final String SIDE_BUY = "1";
    public static final String SIDE_SELL = "2";

    // Status values
    public static final String ORD_STATUS_FILLED = "2";
    public static final String ORD_STATUS_REJECTED = "8";

    private FixTags() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
