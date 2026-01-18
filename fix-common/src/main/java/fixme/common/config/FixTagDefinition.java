package fixme.common.config;

import java.util.Map;

/**
 * Definition of a FIX tag loaded from fix-tags.json
 */


public class FixTagDefinition {

    private String tag;
    private String name;
    private String type;
    private boolean required;
    private String description;
    private Map<String, String> validValues;

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public Map<String, String> getValidValues() {
        return validValues;
    }

    public void setValidValues(Map<String, String> validValues) {
        this.validValues = validValues;
    }

    public boolean isValidValue(String value) {
        if (validValues == null || validValues.isEmpty()) {
            return true;
        }
        return validValues.containsKey(value);
    }

    public String getValueDescription(String value) {
        if (validValues == null) {
            return null;
        }
        return validValues.getOrDefault(value, value);
    }

    @Override
    public String toString() {
        return String.format("Tag %s (%s): %s [%s, %s]",
                tag, name, description, type, required ? "required" : "optional");
}

}