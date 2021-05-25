import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

////////////////////////////////////////////////////////////////////////////////
// NOTE: You are not supposed to modify the code below.
////////////////////////////////////////////////////////////////////////////////

class QuoteLibraryImpl implements QuoteLibrary {
    private final AtomicLong userThreadId = new AtomicLong();

    private final double serviceLambda;
    private final double priceMeanReturn;
    private final double priceVolatility;

    private long lastTickWasAtMillis;
    private double lastTradePrice;
    private long totalDelayMs;
    private long totalTicks;

    // Possible errors.
    private final AtomicInteger registerUserThreadWasCalledMultipleTimes = new AtomicInteger();
    private final AtomicInteger getTickWasCalledFromTheWrongThread = new AtomicInteger();
    private final AtomicInteger getTickWasCalledWithTheWrongTicker = new AtomicInteger();

    public QuoteLibraryImpl(double ticksPerSecond, double priceMeanReturn, double priceVolatility) {
        this.serviceLambda = ticksPerSecond / 1000.0;
        this.priceMeanReturn = priceMeanReturn;
        this.priceVolatility = priceVolatility;

        lastTickWasAtMillis = System.currentTimeMillis();
        lastTradePrice = ThreadLocalRandom.current().nextDouble(2500.0, 3500.0);
        totalDelayMs = 0;
        totalTicks = 0;
    }

    public void registerUserThread() {
        long callerId = Thread.currentThread().getId();
        if (!userThreadId.compareAndSet(0, callerId)) {
            Logging.info("User thread was already registered!");
            registerUserThreadWasCalledMultipleTimes.incrementAndGet();
            return;
        }
        Logging.debug("Registered thread [%d] as the user thread", callerId);
    }

    public Quote.Tick getTick(String ticker) {
        long callerId = Thread.currentThread().getId();
        long userId = userThreadId.get();
        if (callerId != userId) {
            Logging.info(
                    "Invoked the `getTick` method from the wrong thread! (CallerId: %d, UserId: %d)",
                    callerId, userId);
            getTickWasCalledFromTheWrongThread.incrementAndGet();
            throw new IllegalStateException("BOOM! Wrong thread!");
        }

        if (!ticker.equals("SPY")) {
            Logging.info("Invoked the `getTick` method with the wrong ticker! (Ticker: %s)",
                    ticker);
            getTickWasCalledWithTheWrongTicker.incrementAndGet();
            throw new IllegalStateException("BOOM! Wrong ticker!");
        }

        ThreadLocalRandom rng = ThreadLocalRandom.current();

        long delayMs = 1 + (long) (-Math.log(rng.nextDouble()) / serviceLambda);
        lastTickWasAtMillis += delayMs;
        lastTradePrice *= 1.0 + priceMeanReturn + priceVolatility * rng.nextGaussian();
        totalDelayMs += delayMs;
        totalTicks += 1;

        long nowMillis = System.currentTimeMillis();
        if (nowMillis < lastTickWasAtMillis) {
            Logging.debug(
                    "I am a grumpy library, so I am going to sleep for a while... hang on there...");
            try {
                Thread.sleep(lastTickWasAtMillis - nowMillis);
            } catch (InterruptedException e) {
                throw new IllegalStateException("BOOM! Interrupted!");
            }
        }
        Logging.debug("Okay, the tick is: %f @ %d", lastTradePrice, lastTickWasAtMillis);

        return new Quote.Tick(lastTickWasAtMillis, Math.round(lastTradePrice * 10000.0));
    }

    public long getTotalDelayMs() {
        return totalDelayMs;
    }

    public long getTotalTicks() {
        return totalTicks;
    }

    public int getErrorCountWhenRegisterUserThreadWasCalledMultipleTimes() {
        return registerUserThreadWasCalledMultipleTimes.get();
    }

    public int getErrorCountWhenGetTickWasCalledFromTheWrongThread() {
        return getTickWasCalledFromTheWrongThread.get();
    }

    public int getErrorCountWhenGetTickWasCalledWithTheWrongTicker() {
        return getTickWasCalledWithTheWrongTicker.get();
    }
}
