package fixme.common.config;

import java.util.List;

public class FixMessageType {

    private String name;
    private String description;
    private List<String> requiredTags;
    private List<String> optionalTags;

    public FixMessageType() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getRequiredTags() {
        return requiredTags;
    }

    public void setRequiredTags(List<String> requiredTags) {
        this.requiredTags = requiredTags;
    }

    public List<String> getOptionalTags() {
        return optionalTags;
    }

    public void setOptionalTags(List<String> optionalTags) {
        this.optionalTags = optionalTags;
    }

    public Boolean isTagRequired(String tag) {
        return requiredTags != null && requiredTags.contains(tag);
    }

    @Override
    public String toString() {
        return String.format("%s: %s (required: %d, optional: %d)", 
            name, description, 
            requiredTags != null ? requiredTags.size() : 0,
            optionalTags != null ? optionalTags.size() : 0);
    }
}