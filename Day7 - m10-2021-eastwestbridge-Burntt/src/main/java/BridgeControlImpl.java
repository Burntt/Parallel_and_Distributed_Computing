import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class BridgeControlImpl implements BridgeControl {

    private static final int BRIDGE_CAPACITY = 3;

    ConcurrentLinkedQueue<Direction> carsOnBridge = new ConcurrentLinkedQueue<Direction>();
    AtomicInteger numberOfCarsGoingEast = new AtomicInteger(0);
    AtomicInteger numberOfCarsGoingWest = new AtomicInteger(0);
    ReentrantLock barrier = new ReentrantLock();
    Condition bridgeGuard = barrier.newCondition();

    synchronized void addCarGoingEast(Direction direction) throws InterruptedException {
        barrier.lock();
        carsOnBridge.add(direction);
        while ((numberOfCarsGoingEast.get() + numberOfCarsGoingWest.get()  >= BRIDGE_CAPACITY) || (numberOfCarsGoingWest.get() > 0)) {
            bridgeGuard.await();
        }
        numberOfCarsGoingEast.incrementAndGet();
        barrier.unlock();
    }

    synchronized void addCarGoingWest(Direction direction) throws InterruptedException {
        barrier.lock();
        carsOnBridge.add(direction);
        while ((numberOfCarsGoingWest.get() + numberOfCarsGoingEast.get() >= BRIDGE_CAPACITY) || (numberOfCarsGoingEast.get() > 0)) {
            bridgeGuard.await();
        }
        numberOfCarsGoingWest.incrementAndGet();
        barrier.unlock();
    }

    @Override
    public synchronized void onArrive(Direction direction) throws InterruptedException {
        if (direction == Direction.EAST) {
            addCarGoingEast(direction);
        } else {
            addCarGoingWest(direction);
        }
    }

    @Override
    public void onDepart() {
            Direction direction = carsOnBridge.poll();
            barrier.lock();
            if (direction == Direction.EAST) {
                numberOfCarsGoingEast.decrementAndGet();
            } else {
                numberOfCarsGoingWest.decrementAndGet();
            }
            bridgeGuard.signal();
            barrier.unlock();
            System.out.println("Departing car from the " + direction);
    }
}