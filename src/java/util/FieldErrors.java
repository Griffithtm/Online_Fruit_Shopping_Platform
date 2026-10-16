package util;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Validation failure for a whole form: one message per field (NFR-UX-01).
 * The exception message is the first field error, so generic handlers still show something useful.
 */
public final class FieldErrors extends IllegalArgumentException {

    private final Map<String, String> errors;

    private FieldErrors(Map<String, String> errors) {
        super(errors.values().iterator().next());
        this.errors = Collections.unmodifiableMap(errors);
    }

    public Map<String, String> getErrors() {
        return errors;
    }

    /** Runs one field check; a validation error is stored under {@code field} and null is returned. */
    public static <T> T check(Map<String, String> errors, String field, Supplier<T> validation) {
        try {
            return validation.get();
        } catch (IllegalArgumentException e) {
            errors.put(field, e.getMessage());
            return null;
        }
    }

    public static void throwIfAny(Map<String, String> errors) {
        if (!errors.isEmpty()) {
            throw new FieldErrors(new LinkedHashMap<>(errors));
        }
    }

    public static FieldErrors of(String field, String message) {
        Map<String, String> errors = new LinkedHashMap<>();
        errors.put(field, message);
        return new FieldErrors(errors);
    }
}
