public interface RWLock {
    void readLock() throws InterruptedException;
    void readUnlock();

    void writeLock() throws InterruptedException;
    void writeUnlock();
}