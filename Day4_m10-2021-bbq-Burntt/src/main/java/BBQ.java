interface BBQ {
    // Inserts the specified element at the tail of this queue, waiting for space to become
    // available if the queue is full.
    void put(Object o) throws InterruptedException;

    // Retrieves and removes the head of this queue, waiting if necessary until an element becomes
    // available.
    Object take() throws InterruptedException;
}
