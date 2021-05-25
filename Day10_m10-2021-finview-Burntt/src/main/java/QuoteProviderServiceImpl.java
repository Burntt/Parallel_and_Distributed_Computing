import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicStampedReference;

// The class must provide the quote provider service.
// Internally, you must start a background thread that will be used to access
// the provided quote library.
//
class QuoteProviderServiceImpl implements QuoteProviderService {

    protected final QuoteLibrary quoteLibrary;
    protected final String ticker;

    private final AtomicStampedReference<Quote.Tick> previousTick = new AtomicStampedReference<Quote.Tick>(null, 0);
    private final ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorService multiThreadExecutor = Executors.newFixedThreadPool(16);


    public QuoteProviderServiceImpl(QuoteLibrary quoteLibrary, String ticker) {
        this.ticker = ticker;
        this.quoteLibrary = quoteLibrary;

        try {
            singleThreadExecutor.submit(this.quoteLibrary::registerUserThread).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void shutdownAndAwaitTermination() {
        singleThreadExecutor.shutdown();
    }

    @Override
    public CompletableFuture<Quote> getLatestAndPreviousTicks() {
        return CompletableFuture.supplyAsync(() -> CompletableFuture.supplyAsync(this::get, singleThreadExecutor).join(), multiThreadExecutor);
    }

    private Quote get() {
        Quote.Tick getLastTick = this.quoteLibrary.getTick(ticker);
        Quote.Tick getPreviousTick = previousTick.getReference();
        previousTick.set(getLastTick,
                (int) System.currentTimeMillis());
        return new Quote(ticker, getPreviousTick != null ? Arrays.asList(getPreviousTick, getLastTick) : Collections.singletonList(getLastTick));
    }
}
