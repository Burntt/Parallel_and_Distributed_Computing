# Pour & Drink

## Statement

Usually, when one learns network programming, he/she is asked to write a "ping-pong" application.
Like, send "ping" - reply "pong". Too schoolish. In Russia, we write pour-drink applications!
Let one thread pour (black tea, of course!), and the other thread drink!

Write an application with the two threads. The first thread writes "Pour!" into standard output,
the second thread writes "Drink!" into the standard output. The threads must alternate when writing.
The first thread must write first, so the output looks like:

```
Pour!
Drink!
Pour!
Drink!
...
```

In total, there must be 100'000 lines.

## Report

*Q*: How do you synchronize the threads?

*A*: I want my threads to execute in a particular order, therefore the simplest way have thought of is by using a static boolean variable which plays the switcher role for my two threads.

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
