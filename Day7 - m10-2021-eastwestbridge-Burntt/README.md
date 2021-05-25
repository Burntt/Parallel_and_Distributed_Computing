# East West Bridge

## Statement

There is a bridge which is aligned along the east-west direction. This bridge is too narrow
to allow cars to go in both directions. Hence, cars must alternate going across the bridge.
The bridge is also not strong enough to hold more than three cars at a time. Find a solution
to this problem which does not cause starvation. That is, cars that want to get across
should eventually get across. However, we want to maximize use of the bridge. Cars should travel
across to the maximum capacity of the bridge (that is, three cars should go at one time).
If a car leaves the bridge going east and there are no westbound cars, then the next eastbound car
should be allowed to cross. We don't want a solution which moves cars across the bridge three at a time,
i.e., eastbound cars that are waiting should not wait until all three cars that are eastbound
and crossing the bridge have crossed before being permitted to cross.

There is a starter code with a problem model in `src/main/java/EastWestBridgeProblem.java`.
Your goal is to implement the methods of `BridgeControlImpl` class.

The `onArrive` method is called by every car when arriving to the bridge.
The implementation must block the current thread if it is unsafe to proceed to the bridge.

The `onDepart` method is called by every car when leaving the bridge.

Look inside the `Car` class to see how it uses the `BridgeControl` interface.

There are tests in `src/main/test/TestEastWestBridgeProblem.java`.
Test complexity increases with their order, so start writing your implementation and testing it against `testOneCarPasses`.
Then continue to `testBlocksOnBridgeCapacity` and so on.

You need to pass every test.

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
