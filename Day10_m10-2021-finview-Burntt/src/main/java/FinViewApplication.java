import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import static java.lang.String.format;

public class FinViewApplication implements UIEventListener {
    protected static final DecimalFormat VALUE_FORMAT;
    protected static final DecimalFormat CHANGE_FORMAT;

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setDecimalSeparator('.');
        symbols.setGroupingSeparator(' ');
        VALUE_FORMAT = new DecimalFormat("#.####", symbols);
        VALUE_FORMAT.setDecimalSeparatorAlwaysShown(true);
        CHANGE_FORMAT = new DecimalFormat("#.##", symbols);
        CHANGE_FORMAT.setDecimalSeparatorAlwaysShown(true);
        CHANGE_FORMAT.setPositivePrefix("+");
        CHANGE_FORMAT.setNegativePrefix("-");
    }

    protected final QuoteProvider quoteProvider;
    protected final UILibrary uiLibrary;
    private final ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorService multiThreadExecutor = Executors.newFixedThreadPool(16);

    public FinViewApplication(QuoteProvider quoteProvider, UILibrary ui) {
        this.uiLibrary = ui;
        this.uiLibrary.addEventListener(this);
        this.quoteProvider = quoteProvider;

        try {
            singleThreadExecutor.submit(this.uiLibrary::registerRendererThread).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    public void shutdownAndAwaitTermination() {
        if (this.quoteProvider instanceof QuoteProviderService) {
            ((QuoteProviderService) this.quoteProvider).shutdownAndAwaitTermination();
        }
    }

    @Override
    public void refreshButtonClicked() {

        AtomicReference<Float> previousExpense = new AtomicReference<>((float) 0);
        CompletableFuture<Quote> quote = this.quoteProvider.getLatestAndPreviousTicks();

        multiThreadExecutor.execute(() -> {

            List<Quote.Tick> tickList = quote.join().getTicks();
            Quote.Tick prev = tickList.get(0);

            if (tickList.size() > 1) {

                Quote.Tick currentTick = tickList.get(1);

                previousExpense.set((float) prev.getTradePriceTimes10000());
                float expense = (float) currentTick.getTradePriceTimes10000() / 10_000;
                float differenceInExpense = expense / (previousExpense.get() / 100 / 10_000);
                long alreadyTicked = currentTick.getTimestampMillis() - prev.getTimestampMillis();

                singleThreadExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        uiLibrary.render(renderTarget -> renderTarget.renderText(format("SPY: %.3f; ticked %dms ago; changed %s%.2f%%",
                                expense,
                                alreadyTicked,
                                (differenceInExpense > 0 ? "+" : ""),
                                differenceInExpense)));
                    }
                });

            } else {

                float price = ((float) prev.getTradePriceTimes10000()) / 10000;
                singleThreadExecutor.execute(() -> uiLibrary.render(r -> r.renderText(format("SPY: %.3f; ticked 0ms ago", price))));
            }
        });
    }

}
