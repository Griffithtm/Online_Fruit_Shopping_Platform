package model;

/**
 * Typed query or business result: JsonEntry.
 */
public class JsonEntry {

    private String key;
    private String value;

    public JsonEntry() {
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
