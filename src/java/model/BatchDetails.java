package model;

/**
 * Typed query or business result: BatchDetails.
 */
public class BatchDetails extends Batch {

    private String optionName;
    private String title;

    public BatchDetails() {
    }

    public String getOptionName() {
        return optionName;
    }

    public void setOptionName(String optionName) {
        this.optionName = optionName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
