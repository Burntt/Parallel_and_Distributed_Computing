import java.util.function.Consumer;

interface UILibrary {
    // Marks the caller thread as the one who has a permission to render (i. e. to call `render`
    // method).
    // The renderer thread must register itself before making any further calls.
    // The renderer thread may be registered only once.
    void registerRendererThread();

    // Adds an event handler to handle the UI events.
    // Listeners are called from the UI thread, so they must complete as soon as possible.
    void addEventListener(UIEventListener listener);

    // Invokes a render action by calling a provided renderer from within the UI thread.
    // Only the registered rendered thread may call this method (see `registerRendererThread`).
    void render(Consumer<UIRenderTarget> renderer);
}
