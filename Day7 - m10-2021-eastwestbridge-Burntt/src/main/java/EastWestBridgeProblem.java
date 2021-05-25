import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

public class EastWestBridgeProblem {
    public static final int BRIDGE_CAPACITY = 3;

    ////////////////////////////////////////////////////////////////////////////////

    public static void main(String[] args) throws InterruptedException {
        simulate(new BridgeControlImpl(), 30, Duration.ofSeconds(30), 1.0,
                RANDOM_DIRECTION_SUPPLIER, true);
    }

    ////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////
    // Testing code lives below. Do not touch. But you may read.
    ////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////

    public static final Supplier<Direction> RANDOM_DIRECTION_SUPPLIER = () -> {
        if (ThreadLocalRandom.current().nextBoolean()) {
            return Direction.EAST;
        } else {
            return Direction.WEST;
        }
    };

    public static void simulate(BridgeControl control, int carCount, Duration duration, double rho,
            Supplier<Direction> directionSupplier, boolean verbose) throws InterruptedException {
        boolean previousLogDebug = Logging.setDebugLogEnabled(verbose);
        boolean previousinfo = Logging.setInfoLogEnabled(true);

        Logging.debug("Welcome to the East-West Bridge!");

        final double lambda = (double) carCount / (double) duration.toMillis();
        final double mu = lambda / rho;

        long[] arrivalTimesMs = new long[carCount];
        for (int carIndex = 0; carIndex < carCount; ++carIndex) {
            arrivalTimesMs[carIndex] = ThreadLocalRandom.current().nextLong(duration.toMillis());
        }
        Arrays.sort(arrivalTimesMs);

        List<Car> cars = new LinkedList<>();

        for (int carIndex = 0; carIndex < carCount; ++carIndex) {
            Thread.sleep(
                    arrivalTimesMs[carIndex] - (carIndex > 0 ? arrivalTimesMs[carIndex - 1] : 0));
            Direction travelDirection = directionSupplier.get();
            long travelTimeMs = (long) (-Math.log(ThreadLocalRandom.current().nextDouble()) / mu);
            Car car = new Car(control, travelDirection, travelTimeMs);
            car.setName(String.format("Car#%d/%s", carIndex, travelDirection));
            Logging.debug("Spawning %s", car.getName());
            car.start();
            cars.add(car);
        }

        for (Car car : cars) {
            car.join();
        }

        Logging.setDebugLogEnabled(previousLogDebug);
        Logging.setInfoLogEnabled(previousinfo);
    }
}
