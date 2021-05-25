import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;


public class PourDrink {

    public static int MAX_PRINTS = 100_000;
    public static int statusPour0Drink1 = 0;
    static AtomicInteger atomicCounter = new AtomicInteger(1);
    static ReentrantLock mutex = new ReentrantLock();
    static Condition monitor = mutex.newCondition();


    static class FirstThread extends Thread {

        @Override
        public void run() {
            while (atomicCounter.get() < MAX_PRINTS) {
                mutex.lock();
                try {
                    while (statusPour0Drink1 != 0) {
                        monitor.await();
                    }
                    System.out.println("Pour!");
                    atomicCounter.incrementAndGet();
                    statusPour0Drink1 = 1;
                    monitor.signal();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    mutex.unlock();
                }
            }
        }
    }

    static class SecondThread extends Thread {

        @Override
        public void run() {
            while (atomicCounter.get() < MAX_PRINTS) {
                mutex.lock();
                try {
                    while (statusPour0Drink1 != 1) {
                        monitor.await();
                    }
                    System.out.println("Drink!");
                    atomicCounter.incrementAndGet();
                    statusPour0Drink1 = 0;
                    monitor.signal();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    mutex.unlock();
                }
            }
        }
    }

    public void run() throws InterruptedException {

        Thread first = new FirstThread();    // pour state 0
        Thread second = new SecondThread();  // drink state 0

        // Do not touch the code below.
        second.start();
        Thread.sleep(50);
        first.start();
        Thread.sleep(50);
        first.join();
        second.join();
    }

    public static void main(final String[] args) throws InterruptedException {
        new PourDrink().run();
    }

}
