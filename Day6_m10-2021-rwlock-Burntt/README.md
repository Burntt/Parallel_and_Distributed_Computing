# RWLock

## Statement

RWLock stands for a read-write lock.

That's a kind of lock which allows you to distinguish the reader threads and the writer threads,
allowing the readers to operate concurrently, the writers to operate serially, and provide a mutual exclusion
for the readers and the writers.

More formally, the read lock may be held simultaneously by multiple reader threads, so long as there are no writer threads.
The write lock is exclusive.

Write your own read-write lock.

## Report

*Q*: How do you provide mutual exclusion for the readers and the writers?

*A*: With an extra variable 'WRITE_LOCKED', which is set to -1 when there is a write lock present.
 A read lock can be obtained whenever there is no  write lock.

*Q*: For how long the writers could starve if there are a plenty of readeres?

*A*: When there are many more writers than readers, the 'starving writer' can occur. This means that a stream of readers can subsequently lock all potential writers out and starve them. Therefore, in theory this can be indefinetly.

*Q*: Any ideas how to prevent the writer starvation? (No need to code, just explain the idea.)

*A*: We could change the priority, and give the writer a chance to engage in the write after it joins the read/write queue. Thereby, when reader 1 (R1) access the information, then R2, then a writer (W1), and then another reader (R3), the write will happen after the second read (R2)

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
