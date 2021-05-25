public class RWLockImpl implements RWLock {

    static final int WRITE_LOCKED = -1, AVAILABLE = 0;
    private int Reader_Count = AVAILABLE;
    private Thread WriteLockOwner;

    @Override
    public synchronized void readLock() throws InterruptedException {
        while (Reader_Count == WRITE_LOCKED) {
            wait();
        }
        Reader_Count++;
    }

    @Override
    public synchronized void readUnlock() {
        if (Reader_Count <= 0) throw new IllegalMonitorStateException();
        Reader_Count--;
        if (Reader_Count == AVAILABLE) {
            notifyAll();
        }
    }

    @Override
    public synchronized void writeLock() throws InterruptedException {
        while (Reader_Count != AVAILABLE) {
            wait();
        }
        Reader_Count = WRITE_LOCKED;
        WriteLockOwner = Thread.currentThread();
    }

    @Override
    public synchronized void writeUnlock() {
        if (Reader_Count != WRITE_LOCKED || WriteLockOwner != Thread.currentThread()){
            throw new IllegalMonitorStateException();
        }
        Reader_Count = AVAILABLE;
        WriteLockOwner = null;
        notifyAll();
    }
}


class startRWLock {
    public static void main(final String[] args) throws InterruptedException {
        System.out.println("test successfull");
    }
}

