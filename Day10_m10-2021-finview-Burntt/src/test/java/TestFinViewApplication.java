import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

public class TestFinViewApplication {
    private static final Pattern RENDERED_TEXT_PATTERN =
            Pattern.compile("^SPY: \\d+\\.\\d+; ticked \\d+ms ago(?:; changed [+-]\\d+.\\d+%)?$");

    static class Stepper {
        private static final long STEPPER_SLEEP_MS = 100;
        private static final long STEPPER_TIMEOUT_MS = 60_000;

        private final AtomicInteger counter = new AtomicInteger();

        public void mark() {
            counter.incrementAndGet();
        }

        public void waitUntil(int level) throws InterruptedException {
            long deadlineMillis = System.currentTimeMillis() + STEPPER_TIMEOUT_MS;
            while (System.currentTimeMillis() < deadlineMillis && counter.get() < level) {
                Thread.sleep(STEPPER_SLEEP_MS);
            }
            if (counter.get() < level) {
                fail("stepper timed out");
            }
        }
    }

    void assertRenderedTextIsCorrect(String text) {
        assertTrue(RENDERED_TEXT_PATTERN.matcher(text).matches(),
                "rendered text [" + text + "] must match the pattern");
    }

    @Test
    void testThreeClicks() throws InterruptedException {
        QuoteLibraryImpl ql = new QuoteLibraryImpl(1.0, 0.003, 0.02);
        UILibraryImpl ui = new UILibraryImpl();
        Stepper s = new Stepper();
        ui.addEventListener(new UIEventListener() {
            @Override
            public void rendered() {
                s.mark();
            }
        });
        QuoteProviderService qps = ReplacesUtils.instance(QuoteProviderServiceImpl.class, ql, "SPY");
        FinViewApplication app = ReplacesUtils.instance(FinViewApplication.class, qps, ui);

        ui.clickRefreshButton();
        s.waitUntil(1);
        assertRenderedTextIsCorrect(ui.getRenderedText());
        ui.clickRefreshButton();
        s.waitUntil(2);
        assertRenderedTextIsCorrect(ui.getRenderedText());
        ui.clickRefreshButton();
        s.waitUntil(3);
        assertRenderedTextIsCorrect(ui.getRenderedText());

        app.shutdownAndAwaitTermination();
        qps.shutdownAndAwaitTermination();
        ui.shutdownAndAwaitTermination();

        assertEquals(0, ui.getErrorCountWhenRegisterRendererThreadWasCalledMultipleTimes());
        assertEquals(0, ui.getErrorCountWhenRenderWasCalledFromTheWrongThread());
        assertEquals(0, ql.getErrorCountWhenRegisterUserThreadWasCalledMultipleTimes());
        assertEquals(0, ql.getErrorCountWhenGetTickWasCalledFromTheWrongThread());
        assertEquals(0, ql.getErrorCountWhenGetTickWasCalledWithTheWrongTicker());
    }

    @Test
    void testTimings() throws InterruptedException {
        int clickCount = 1000;
        int expectedDurationSeconds = 15;

        QuoteLibraryImpl ql = new QuoteLibraryImpl(
                (double) clickCount / (double) expectedDurationSeconds, 0.003, 0.02);
        UILibraryImpl ui = new UILibraryImpl();
        Stepper s = new Stepper();
        ui.addEventListener(new UIEventListener() {
            @Override
            public void rendered() {
                s.mark();
            }
        });
        QuoteProviderService qps = ReplacesUtils.instance(QuoteProviderServiceImpl.class, ql, "SPY");
        FinViewApplication app = ReplacesUtils.instance(FinViewApplication.class, qps, ui);

        boolean debugLevelWasEnabled = Logging.setDebugLogEnabled(false);
        boolean infoLevelWasEnabled = Logging.setInfoLogEnabled(false);
        for (int i = 0; i < clickCount; ++i) {
            ui.clickRefreshButton();
        }
        s.waitUntil(clickCount);
        Logging.setDebugLogEnabled(debugLevelWasEnabled);
        Logging.setInfoLogEnabled(infoLevelWasEnabled);

        assertTrue(ui.getListenerQ95DelayNs() < 10_000_000,
                "listener calls do not block for more than 10ms");
        assertTrue(ui.getRendererQ95DelayNs() < 10_000_000,
                "renderer calls do not block for more than 10ms");

        app.shutdownAndAwaitTermination();
        qps.shutdownAndAwaitTermination();
        ui.shutdownAndAwaitTermination();

        assertEquals(0, ui.getErrorCountWhenRegisterRendererThreadWasCalledMultipleTimes());
        assertEquals(0, ui.getErrorCountWhenRenderWasCalledFromTheWrongThread());
        assertEquals(0, ql.getErrorCountWhenRegisterUserThreadWasCalledMultipleTimes());
        assertEquals(0, ql.getErrorCountWhenGetTickWasCalledFromTheWrongThread());
        assertEquals(0, ql.getErrorCountWhenGetTickWasCalledWithTheWrongTicker());
    }
}
