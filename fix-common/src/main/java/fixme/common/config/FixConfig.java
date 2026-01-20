package fixme.common.config;

import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;


/**
 * Configuration FIX loaded from fix-tags.json
 *  Singleton class to hold FIX configuration data
 */


public class FixConfig {
    private static final String CONFIG_FILE = "fix-tags.json";    
    private static final Logger logger = LoggerFactory.getLogger(FixConfig.class);

    private static FixConfig instance;


    private String protocol;
    private String version;
    private String description;
    private String delimiter;

    private Map<String, FixTagDefinition> tagDefinitions;
    private Map<String, FixMessageType> messageTypes;
    private Map<String, String> tagNameToNumber;
    
    private FixConfig() {
        this.tagDefinitions = new HashMap<>();
        this.messageTypes = new HashMap<>();
        this.tagNameToNumber = new HashMap<>();
        loadConfiguration();
    }

    public static FixConfig getInstance() {
        if (instance == null) {
            instance = new FixConfig();
        }
        return instance;
    }

    private void loadConfiguration() {

        logger.info("Loading FIX configuration from {}", CONFIG_FILE);

        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (inputStream == null) {
                throw new RuntimeException("Configuration file not found: " + CONFIG_FILE);
            }

            Gson gson = new Gson();
            JsonObject root = gson.fromJson(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8), 
                JsonObject.class
            );


            this.protocol = root.get("protocol").getAsString();
            this.version = root.get("version").getAsString();
            this.description = root.get("description").getAsString();
            this.delimiter = root.get("delimiter").getAsString();

            logger.info("FIX Config - Protocol: {}, Version: {}, Description: {}, Delimiter: '{}'",
                    protocol, version, description, delimiter);
        

            JsonObject tagsObject = root.getAsJsonObject("tags");
            loadTagCategory(gson, tagsObject, "system");
            loadTagCategory(gson, tagsObject, "order");
            loadTagCategory(gson, tagsObject, "execution");

            logger.info("Loaded {} tag definitions", tagDefinitions.size());

            JsonObject msgTypesObject = root.getAsJsonObject("messageTypes");
            loadMessageTypes(gson, msgTypesObject);

            logger.info("Loaded {} message types", messageTypes.size());
            logger.info("FIX configuration loaded successfully");

        } catch (Exception e) {
            logger.error("Error loading FIX configuration", e);
            throw new RuntimeException("Error loading FIX configuration", e);
        }
    }

    private void loadTagCategory(Gson gson, JsonObject tags, String category) {
        if (!tags.has(category)) {
            logger.warn("No '{}' category found in tags", category);
            return;
        }

        JsonArray categoryArray = tags.getAsJsonArray(category);

        for (JsonElement element : categoryArray) {
            FixTagDefinition tagDef = gson.fromJson(element, FixTagDefinition.class);
            tagDefinitions.put(tagDef.getTag(), tagDef);
            tagNameToNumber.put(tagDef.getName(), String.valueOf(tagDef.getTag()));
        }
    }

    private void loadMessageTypes(Gson gson, JsonObject msgTypes) {
        for (Map.Entry<String, JsonElement> entry : msgTypes.entrySet()) {
            String msgType = entry.getKey();
            FixMessageType type = gson.fromJson(entry.getValue(), FixMessageType.class);
            messageTypes.put(msgType, type);
        }
    }

    public String getProtocol() {
        return protocol;
    }
    public String getVersion() {
        return version;
    }
    public String getDescription() {
        return description;
    }
    public String getDelimiter() {
        return delimiter;
    }

    public FixTagDefinition getTagDefinition(String tag) {
        return tagDefinitions.get(tag);
    }

    public String getTagNumber(String tagName) {
        return tagNameToNumber.get(tagName);
    }

    public FixMessageType getMessageType(String msgType) {
        return messageTypes.get(msgType);
    }

    public boolean isRequired(String tag) {
        FixTagDefinition def = tagDefinitions.get(tag);
        return def != null && def.isRequired();
    }

    public boolean isValidValue(String tag, String value) {
        FixTagDefinition def = tagDefinitions.get(tag);
        return def != null && def.isValidValue(value);
    }

    public String getTagDescription(String tag) {
        FixTagDefinition def = tagDefinitions.get(tag);
        return def != null ? def.getDescription() : "Unknown tag";
    }

    public Map<String, FixTagDefinition> getAllTagDefinitions() {
        return tagDefinitions;
    }

    public void printConfigurationSummary() {
        logger.info("FIX Configuration Summary:");
        logger.info("Protocol: {}", protocol);
        logger.info("Version: {}", version);
        logger.info("Description: {}", description);
        logger.info("Delimiter: '{}'", delimiter);
        logger.info("Total Tag Definitions: {}", tagDefinitions.size());
        logger.info("Total Message Types: {}", messageTypes.size());

        logger.info("Tag Definitions:");
        tagDefinitions.values().forEach(def -> 
            System.out.println("  " + def)
        );

        messageTypes.values().forEach(type -> 
            System.out.println("  " + type)
        );
    }


}
