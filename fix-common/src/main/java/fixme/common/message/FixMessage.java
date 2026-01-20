package fixme.common.message;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fixme.common.config.FixConfig;

public class FixMessage {

    private static final Logger logger = LoggerFactory.getLogger(FixMessage.class);
    private static final FixConfig config = FixConfig.getInstance();

    private final Map<String, String> fields;

    public FixMessage() {
        this.fields = new LinkedHashMap<>();
    }

    public void setField(String tag, String value) {
        if (!config.isValidValue(tag, value)) {
            logger.warn("Invalid value '{}' for tag {}", value, tag);
        }
        fields.put(tag, value);
    }

    public String getField(String tag) {
        return fields.get(tag);
    }

    public Map<String, String> getAllFields() {
        return fields;
    }

    public String getSenderCompId() {
        return getField(FixTags.SENDER_COMP_ID);
    }

    public void setSenderCompId(String senderCompId) {
        setField(FixTags.SENDER_COMP_ID, senderCompId);
    }

    public String getTargetCompId() {
        return getField(FixTags.TARGET_COMP_ID);
    }

    public void setTargetCompId(String targetCompId) {
        setField(FixTags.TARGET_COMP_ID, targetCompId);
    }

    public String getMsgType() {
        return getField(FixTags.MSG_TYPE);
    }

    public void setMsgType(String msgType) {
        setField(FixTags.MSG_TYPE, msgType);
    }

    public String getSymbol() {
        return getField(FixTags.SYMBOL);
    }

    public void setSymbol(String symbol) {
        setField(FixTags.SYMBOL, symbol);
    }

    public Boolean isBuyOrder() {
        return FixTags.MSG_TYPE_NEW_ORDER.equals(getMsgType()) &&
               FixTags.SIDE_BUY.equals(getField(FixTags.SIDE));
    }

    public Boolean isSellOrder() {
        return FixTags.MSG_TYPE_NEW_ORDER.equals(getMsgType()) &&
               FixTags.SIDE_SELL.equals(getField(FixTags.SIDE));
    }

    public Boolean isExecutionReport() {
        return FixTags.MSG_TYPE_EXECUTION_REPORT.equals(getMsgType());
    }

    public String calculateChecksum() {
        StringBuilder sb = new StringBuilder();
        String delimiter = config.getDelimiter();
        
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            if (!FixTags.CHECKSUM.equals(entry.getKey())) {
                sb.append(entry.getKey())
                  .append("=")
                  .append(entry.getValue())
                  .append(delimiter);
            }
        }
        
        int sum = 0;
        for (char c : sb.toString().toCharArray()) {
            sum += c;
        }
        
        int checksum = sum % 256;

        logger.debug("⚠️ Calculated checksum: {}", String.format("%03d", checksum));
        return String.format("%03d", checksum);
    }

    public boolean isChecksumValid() {
        String receivedChecksum = getField(FixTags.CHECKSUM);
        if (receivedChecksum == null) {
            return false;
        }
        String calculatedChecksum = calculateChecksum();
        return calculatedChecksum.equals(receivedChecksum);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        String delimiter = config.getDelimiter();
        
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            sb.append(entry.getKey())
              .append("=")
              .append(entry.getValue())
              .append(delimiter);
        }
        
        return sb.toString();
    }

    public static FixMessage parse(String raw) {
        FixMessage message = new FixMessage();
        
        if (raw == null || raw.isEmpty()) {
            return message;
        }
        
        String delimiter = config.getDelimiter();
        String delimiterRegex = delimiter.equals("|") ? "\\|" : delimiter;
        
        String[] parts = raw.split(delimiterRegex);
        
        for (String part : parts) {
            if (part.isEmpty()) continue;
            
            String[] keyValue = part.split("=", 2);
            if (keyValue.length == 2) {
                message.setField(keyValue[0], keyValue[1]);
            }
        }
        
        return message;
    }

    
}

