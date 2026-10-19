package util;

import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/** Stored timestamps are UTC wall-clock values (NFR-DATA-01); screens show Vietnam time. */
public final class VietnamTime {

    private static final ZoneId VIETNAM = ZoneId.of("Asia/Ho_Chi_Minh");

    private VietnamTime() {
    }

    /** Formats a UTC timestamp read from SQL Server with the given pattern, e.g. "dd/MM/yyyy HH:mm"; null stays null. */
    public static String format(Timestamp utc, String pattern) {
        return utc == null ? null : utc.toLocalDateTime().atOffset(ZoneOffset.UTC).atZoneSameInstant(VIETNAM).format(DateTimeFormatter.ofPattern(pattern));
    }
}
