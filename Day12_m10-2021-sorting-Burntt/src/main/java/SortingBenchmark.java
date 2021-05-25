import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.commons.math3.stat.inference.TTest;

public class SortingBenchmark {
    public static void main(String[] args) throws InterruptedException {
        Logging.setDebugLogEnabled(false);
        Logging.setInfoLogEnabled(true);

        Logging.info("Welcome to the Sorting problem!");
        mainForProblemSize(1_000_000, 10); // Aim to have a tie here.
        mainForProblemSize(10_000_000, 10);
        mainForProblemSize(100_000_000, 10); // Aim to win here.
    }

    private static void mainForProblemSize(int problemSize, int runCount)
            throws InterruptedException {
        Logging.info("===== PROBLEM SIZE %d =====", problemSize);

        ThreadLocalRandom rng = ThreadLocalRandom.current();

        double[] firstTimings = new double[runCount];
        double[] secondTimings = new double[runCount];
        SummaryStatistics firstStatistics = new SummaryStatistics();
        SummaryStatistics secondStatistics = new SummaryStatistics();

        Sorting sorting = new Sorting();

        for (int run = 0; run < runCount; ++run) {
            Logging.debug("Run %d: Generating data...", run);
            double[] first = new double[problemSize];
            double[] second = new double[problemSize];
            for (int index = 0; index < problemSize; ++index) {
                double value = rng.nextDouble(1, 1_000_000);
                first[index] = value;
                second[index] = value;
            }

            Logging.debug("Run %d: Ready!", run);

            long startedAt1 = System.nanoTime();
            Arrays.sort(first);
            long duration1 = (System.nanoTime() - startedAt1) / 1_000_000;
            Logging.debug("Run %d: Arrays.sort: %sms", run, duration1);

            long startedAt2 = System.nanoTime();
            sorting.yourSuperSortAlgorithm(second);
            long duration2 = (System.nanoTime() - startedAt2) / 1_000_000;
            Logging.debug("Run %d: yourSuperSortAlgorithm: %sms", run, duration2);

            Logging.debug("Run %d: Checking...", run);
            check(second);
            Logging.debug("Run %d: Ok!", run);

            firstTimings[run] = duration1;
            firstStatistics.addValue(duration1);
            secondTimings[run] = duration2;
            secondStatistics.addValue(duration2);
        }

        Logging.info("*** Arrays.sort @ Problem Size %d", problemSize);
        Logging.info("   %8.3fms +- %8.3fms", firstStatistics.getMean(),
                firstStatistics.getStandardDeviation());
        Logging.info("*** yourSuperSortAlgorithm @ Problem Size %d", problemSize);
        Logging.info("   %8.3fms +- %8.3fms", secondStatistics.getMean(),
                secondStatistics.getStandardDeviation());

        double pValue = new TTest().tTest(firstTimings, secondTimings);
        double tValue = new TTest().t(firstTimings, secondTimings);
        if (pValue < 0.05) {
            Logging.info("IT's not a TIE! (p-value: %.4f)", pValue);
            if (tValue >= 0) {
                Logging.info("WINNER IS: yourSuperSortAlgorithm! (p-value: %.4f)", pValue / 2);
            } else {
                Logging.info("WINNER IS: Arrays.sort! (p-value: %.4f)", pValue / 2);
            }
        } else {
            Logging.info("IT'S A TIE! (p-value: %.4f)", pValue);
        }
    }

    private static void check(double[] values) {
        for (int index = 1; index < values.length; ++index) {
            if (values[index] < values[index - 1]) {
                Logging.info("SOLUTION IS INCORRECT: !([%d] < [%d]) ; !(%s < %s)", index - 1, index,
                        values[index - 1], values[index]);
                System.exit(1);
            }
        }
    }
}
