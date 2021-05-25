interface UIEventListener {
    // Invoked, once the user clicked the "Refresh" button.
    default void refreshButtonClicked() {
    }

    // Invoked, once the UI was re-rendered.
    default void rendered() {
    }
}
