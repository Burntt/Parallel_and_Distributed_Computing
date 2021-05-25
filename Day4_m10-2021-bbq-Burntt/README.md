# BBQ

## Statement

BBQ stands for a bounded blocking queue (FIFO).
"Bounded" means that the queue size is limited (as specified in the constructor argument).
"Blocking" means that the `put` and `take` operations block the current thread if the queue is full/empty.

Write your own bounded blocking queue. You may use an array as a backing storage.

## Report

A Bounded Blocking Queue is a queue that blocks when:
- Dequeueing when the queue is empty
- Enqueing items when the queue is full
The java.lang.Object.notifyAll() wakes up all threads that are waiting on this object's monitor.

A thread trying to dequeue from an empty queue cant continue until some other thread tries to insert an item in to the queue. A thread trying to enqueue an item in a full queue stays blocked until some other thread makes space in the queue.

The notifyAll() method is only called in put and take when the queue size is 0 or equal to capacity. If this is not the case, no threads are waiting to either or dequeue items.

In order to deal with the IllegalMonitorStateException, you must verify that all invocations of the wait method are taking place only when the calling thread owns the appropriate monitor. In order to deal with this, both put() and take() methods require a synchronization block around it.

*Q*: How do you synchronize put and take calls in the case when the queue is empty?

*A*: 

When the queue is empty, the queue should block the take() method. Therefore, a while loop is implemented which puts the asking thread in a blocking mode when the queue is empty.

When in this blocking state an object is put into the queue, and the size of the queue is zero, it notifies all waiting threads on the objects monitor, resulting in an instant take() call.


*Q*: How do you synchronize put and take calls in the case when the queue is full?

*A*: 

When the queue is full, the queue should block the put method. The same reasonsing from the previous question applies but then for this method.


## Help

## How to submit the assignment
1. Accept the invitation link. Since you are reading this, this is likely done.
2. Just commit your code straight to the `master` branch.
3. Check that your code passes all the checks. Look for red/yellow/green labels in the "Commits" view.
4. Once you have finished coding, fill the report below, inside `README.md`.
5. Check for any comments in the PR #1 (called "Feedback").

### How to build the assignment without an IDE

```
gradle build
```

### How to test the assignment without an IDE

```
gradle test
```

### How to run a particular Java class
```
# From the main source set.
java -cp ./build/classes/java/main/ FullyQualifiedClassName
# From the test source set.
java -cp ./build/classes/java/test/ FullyQualifiedClassName
```
