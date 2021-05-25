import java.util.ArrayList;
import java.util.List;

// CAMAAAAAN

class BBQImpl implements BBQ {

    private List<Object> queue = new ArrayList<Object>();
    private final int capacity;

    public BBQImpl(int capacity) {
        this.capacity = capacity;
    }

    @Override
    public void put(Object o) throws InterruptedException {
        synchronized (this) {
            while (this.queue.size() == this.capacity) {
                wait();
            }
            if (this.queue.size() == 0) {
                notifyAll();
            }
            this.queue.add(o);
        }
    }

    @Override
    public Object take() throws InterruptedException {
        synchronized (this) {
            while (this.queue.size() == 0) {
                wait();
            }
            if (this.queue.size() == this.capacity) {
                notifyAll();
            }
            return this.queue.remove(0);
        }
    }

}

class startBBQ {
    public static void main(final String[] args) throws InterruptedException {
        BBQ q = new BBQImpl(2);
        q.put("hello");
        q.put("world");

        System.out.println(q.take());
        System.out.println(q.take());
    }
}
