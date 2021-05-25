import static org.junit.jupiter.api.Assertions.fail;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Test;

public class TestSorting {
    private static void check(double[] values) {
        for (int index = 1; index < values.length; ++index) {
            if (values[index] < values[index - 1]) {
                fail(String.format("SOLUTION IS INCORRECT: !([%d] < [%d]) ; !(%s < %s)", index - 1,
                        index, values[index - 1], values[index]));
            }
        }
    }

    @Test
    public void testCorrectness() throws InterruptedException {
        double[] input = new double[10];
        for (int index = 0; index < input.length; ++index) {
            double value = ThreadLocalRandom.current().nextDouble(1, 1_000_000);
            input[index] = value;
        }

        Sorting sorting = ReplacesUtils.instance(Sorting.class);
        sorting.yourSuperSortAlgorithm(input);

        check(input);
    }
}
