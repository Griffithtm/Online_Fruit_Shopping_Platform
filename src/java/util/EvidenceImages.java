package util;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;

/**
 * Delivery evidence photos (FE-07.2, BA-35/BA-47, ThanhNC 2.6a). Files live outside the public /media folder and are
 * served only by DeliveryServlet after an ownership check. Only JPG/PNG are readable by ImageIO; everything is stored as PNG.
 */
public final class EvidenceImages {

    public static final long MAX_BYTES = 5L * 1024 * 1024;
    public static final String PREFIX = "/delivery/evidence/";
    private static final int MAX_PIXELS = 6000;
    private static final Pattern NAME = Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.png");

    private EvidenceImages() {
    }

    private static Path dir() {
        return Path.of(System.getProperty("catalina.base", System.getProperty("java.io.tmpdir")), "freshora-uploads", "evidence");
    }

    /** True for a stored evidence name such as 1b4e…-….png; anything else (including "../") is rejected. */
    public static boolean validName(String name) {
        return name != null && NAME.matcher(name).matches();
    }

    /** True for a path produced by {@link #save}. */
    public static boolean validPath(String path) {
        return path != null && path.startsWith(PREFIX) && validName(path.substring(PREFIX.length()));
    }

    /** Checks size and content (MSG60 when unusable), converts to PNG and returns its route path. */
    public static String save(InputStream in, long size) throws IOException {
        if (size <= 0 || size > MAX_BYTES) {
            throw Messages.error(Messages.MSG60);
        }
        BufferedImage image = ImageIO.read(in);
        if (image == null || image.getWidth() > MAX_PIXELS || image.getHeight() > MAX_PIXELS) {
            throw Messages.error(Messages.MSG60);
        }
        String name = UUID.randomUUID() + ".png";
        Files.createDirectories(dir());
        ImageIO.write(image, "png", dir().resolve(name).toFile());
        return PREFIX + name;
    }

    /** The stored file of a valid route path, or null. */
    public static Path file(String path) {
        return validPath(path) ? dir().resolve(path.substring(PREFIX.length())) : null;
    }

    /** Removes a saved file that ended up unused (failed or repeated delivery confirmation). */
    public static void delete(String path) {
        Path p = file(path);
        if (p != null) {
            try {
                Files.deleteIfExists(p);
            } catch (IOException ignored) {
            }
        }
    }
}
