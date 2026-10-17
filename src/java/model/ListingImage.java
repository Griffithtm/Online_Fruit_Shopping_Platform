package model;

/**
 * Typed query or business result: ListingImage.
 */
public class ListingImage {

    private String position;
    private String path;

    public ListingImage() {
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
