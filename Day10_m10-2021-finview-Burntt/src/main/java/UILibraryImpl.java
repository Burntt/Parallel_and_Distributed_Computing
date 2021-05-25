import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

////////////////////////////////////////////////////////////////////////////////
// NOTE: You are not supposed to modify the code below.
////////////////////////////////////////////////////////////////////////////////

class UILibraryImpl implements UILibrary {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // Kinda UI state.
    private final CopyOnWriteArrayList<UIEventListener> listeners = new CopyOnWriteArrayList<>();
    private final AtomicLong rendererThreadId = new AtomicLong();
    private String renderedText = null;

    // Possible errors.
    private final AtomicInteger registerRendererThreadWasCalledMultipleTimes = new AtomicInteger();
    private final AtomicInteger renderWasCalledFromTheWrongThread = new AtomicInteger();
    private final List<Long> listenerDelayNs = new ArrayList<>();
    private final List<Long> rendererDelayNs = new ArrayList<>();

    public void shutdownAndAwaitTermination() {
        try {
            Logging.debug("UI library is shutting down...");
            executor.shutdown();
            executor.awaitTermination(15, TimeUnit.SECONDS);
            Logging.debug("UI library was shut down.");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    class UIRenderTargetImpl implements UIRenderTarget {
        public void renderText(String text) {
            renderedText = text;
            Logging.debug("Rendered text [%s]", renderedText);
        }
    }

    @Override
    public void registerRendererThread() {
        long callerId = Thread.currentThread().getId();
        if (!rendererThreadId.compareAndSet(0, callerId)) {
            Logging.info("Renderer thread was already registered!");
            registerRendererThreadWasCalledMultipleTimes.incrementAndGet();
            return;
        }
        Logging.debug("Registered thread [%d] as the renderer thread", callerId);
    }

    @Override
    public void addEventListener(UIEventListener listener) {
        listeners.add(listener);
        Logging.debug("Added an event listener");
    }

    @Override
    public void render(Consumer<UIRenderTarget> renderer) {
        long callerId = Thread.currentThread().getId();
        long rendererId = rendererThreadId.get();
        if (callerId != rendererId) {
            Logging.info(
                    "Invoked the `render` method from the wrong thread! (CallerId: %d, RendererId: %d)",
                    callerId, rendererId);
            renderWasCalledFromTheWrongThread.incrementAndGet();
            return;
        }
        Logging.debug("Scheduling the renderer to be executed");
        executor.execute(() -> {
            invokeRenderer(renderer);
            invokeListeners("rendered", UIEventListener::rendered);
        });
    }

    public void clickRefreshButton() {
        Logging.debug("Refresh button was clicked");
        executor.execute(() -> {
            invokeListeners("refreshButtonClicked", UIEventListener::refreshButtonClicked);
        });
    }

    private void invokeRenderer(Consumer<UIRenderTarget> renderer) {
        long beginNs = System.nanoTime();
        renderer.accept(new UIRenderTargetImpl());
        long endNs = System.nanoTime();
        rendererDelayNs.add(endNs - beginNs);
        Logging.debug("Renderer took %dns to complete", endNs - beginNs);
    }

    private void invokeListeners(String name, Consumer<UIEventListener> event) {
        long beginNs = System.nanoTime();
        listeners.forEach(event);
        long endNs = System.nanoTime();
        listenerDelayNs.add(endNs - beginNs);
        Logging.debug("Listeners for '%s' took %dns to complete", name, endNs - beginNs);
    }

    private long getQ95(List<Long> values) {
        try {
            return executor.submit(() -> {
                long result = 0;
                if (!values.isEmpty()) {
                    values.sort(null);
                    int position = values.size() * 95 / 100;
                    position = Math.min(values.size() - 1, position);
                    position = Math.max(0, position);
                    result = values.get(position);
                }
                return result;
            }).get();
        } catch (InterruptedException | ExecutionException e) {
            return 0;
        }
    }

    public int getErrorCountWhenRegisterRendererThreadWasCalledMultipleTimes() {
        return registerRendererThreadWasCalledMultipleTimes.get();
    }

    public int getErrorCountWhenRenderWasCalledFromTheWrongThread() {
        return renderWasCalledFromTheWrongThread.get();
    }

    public long getListenerQ95DelayNs() {
        return getQ95(listenerDelayNs);
    }

    public long getRendererQ95DelayNs() {
        return getQ95(rendererDelayNs);
    }

    public String getRenderedText() {
        try {
            return executor.submit(() -> {
                return renderedText;
            }).get();
        } catch (InterruptedException | ExecutionException e) {
            return null;
        }
    }
}
