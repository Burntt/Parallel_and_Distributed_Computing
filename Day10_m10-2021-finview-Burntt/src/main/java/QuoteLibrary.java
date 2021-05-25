interface QuoteLibrary {
    // Marks the caller thread as the one who has a permission to ask for a new tick (i. e. to call `getTick` method).
    // The user thread must register itself before making any further calls.
    // The user thread may be registered only once.
    void registerUserThread();

    // Returns the next tick for a given ticker.
    // Note: only SPY is supported right now.
    Quote.Tick getTick(String ticker);
}