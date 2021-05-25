import java.util.concurrent.atomic.AtomicBoolean;

public class Logging {
    private final static long STARTED_AT = System.currentTimeMillis();
    private final static AtomicBoolean LOG_DEBUG_IS_ENABLED = new AtomicBoolean(true);
    private final static AtomicBoolean LOG_INFO_IS_ENABLED = new AtomicBoolean(true);

    public static boolean setDebugLogEnabled(boolean value) {
        return LOG_DEBUG_IS_ENABLED.getAndSet(value);
    }

    public static boolean setInfoLogEnabled(boolean value) {
        return LOG_INFO_IS_ENABLED.getAndSet(value);
    }

    public static void debug(String format, Object... args) {
        if (LOG_DEBUG_IS_ENABLED.get()) {
            log('D', format, args);
        }
    }

    public static void info(String format, Object... args) {
        if (LOG_INFO_IS_ENABLED.get()) {
            log('I', format, args);
        }
    }

    private static void log(char level, String format, Object... args) {
        long delta = System.currentTimeMillis() - STARTED_AT;
        String message = String.format(format, args);
        System.out.printf("%c [+%8dms] T[%-12s]: %s%n", level, delta, Thread.currentThread().getName(), message);
    }

    private Logging() {
    }
}