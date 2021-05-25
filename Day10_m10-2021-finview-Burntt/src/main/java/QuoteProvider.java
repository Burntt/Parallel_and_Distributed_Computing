import java.util.concurrent.CompletableFuture;

interface QuoteProvider {
    // Returns the ticker as well as the latest and previous-to-the-latest quoted ticks.
    //
    // Each invocation of this method must lead to a call to the underlying quote
    // library, its `getTick` method.
    //
    // The method must return immediately (therefore it returns the CompletableFuture).
    //
    // The future must complete when the new tick is provided by the underlying quote library.
    //
    // The resulting class -- TickerAndQuotes -- must contain at most two ticks.
    //
    // See the tests for a formal specification of the behaviour.
    CompletableFuture<Quote> getLatestAndPreviousTicks();
}
