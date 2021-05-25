import java.util.Collections;
import java.util.List;

class Quote {
    static class Tick {
        private final long timestampMillis;
        private final long tradePriceTimes10000;

        public Tick(long timestampMillis, long tradePriceTimes10000) {
            this.timestampMillis = timestampMillis;
            this.tradePriceTimes10000 = tradePriceTimes10000;
        }

        public long getTimestampMillis() {
            return timestampMillis;
        }

        public long getTradePriceTimes10000() {
            return tradePriceTimes10000;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (int) (timestampMillis ^ (timestampMillis >>> 32));
            result = prime * result + (int) (tradePriceTimes10000 ^ (tradePriceTimes10000 >>> 32));
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Tick other = (Tick) obj;
            if (timestampMillis != other.timestampMillis)
                return false;
            if (tradePriceTimes10000 != other.tradePriceTimes10000)
                return false;
            return true;
        }

        @Override
        public String toString() {
            return "Tick [timestampMillis=" + timestampMillis + ", tradePriceTimes10000="
                    + tradePriceTimes10000 + "]";
        }
    }

    private final String ticker;
    private final List<Quote.Tick> ticks;

    public Quote(String ticker, List<Quote.Tick> ticks) {
        this.ticker = ticker;
        this.ticks = ticks;
    }

    public String getTicker() {
        return ticker;
    }

    public List<Quote.Tick> getTicks() {
        return Collections.unmodifiableList(ticks);
    }
}
