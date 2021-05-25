interface QuoteProviderService extends QuoteProvider {
    // Shuts down internal threads and waits for their termination.
    void shutdownAndAwaitTermination();
}
