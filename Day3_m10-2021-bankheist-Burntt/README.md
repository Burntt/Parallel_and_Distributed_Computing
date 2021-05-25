# Bank Heist

## Statement

Good morning, Mr. Jaffett!

It's rumored that Michael De Santa is attempting a heist, again.
His plan is to trigger the concurrency bug in Javashire Getaway systems.
We have called our IT department to revise the code, and here is what we have learned.

In our bank each client has two paired accounts.
The client may request to transfer funds between the accounts.
Transfers are allowed only if the source account has a positive balance.
The client may request to withdraw funds from either of the accounts.
To be client-friendly, we allow to withdraw funds from any account given the total non-negative balance of all client accounts after the transaction.
This is exactly the feature De Santa is rumored to exploit.

Our best engineers (they are taking a serious training in parallel computing!) are asked to harden the system.
We'll keep you informed.

All the best,
I. P.

---

Dear student!

Would you please help Mr. Jaffett by writing a safe code for the bank system?

Thanks!
I. P.

## Report

*Q*: How do you ensure safety in your code?

*A*: I use synchronization to avoid unwanted executions, by establishing ordering constraints on transfer and withdraw. Separetely for each balance A & B.

*Q*: Run the test code with a different number of threads. Estimate the parallizable fraction of work, as in Amdahl's law. Estimate the theoretical speed-up..

*A*: 
- 1 Threads: 625 ms
- 2 Threads: 567 ms
- 4 Threads: 447 ms
- 8 Threads: 956 ms ??
- 16 Threads: 920 ms ??

--- from here is useless ---

- 32 Threads: 895 ms
- 64 Threads: 888 ms

Therefore:
- 1 -> 2 : +10% speed up
- 2 > 3 : +20% speed up
- rest: speed down? why?

Estimated parallel portion: 50%

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
