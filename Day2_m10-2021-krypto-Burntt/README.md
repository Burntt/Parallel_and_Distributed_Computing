# Krypto

## Statement

Let's call string S *alpha* if it consists only from lowercase English letters (that is from 'a' to 'z').

Let's call string S *subtle* if its SHA-256 hash digest starts with "202005" (if written in a hex form).

Write a multi-threaded program that finds for every character *C* from `'a'` to `'z'`
the shortest alpha subtle string starting with that character *C*.
Print these 26 strings to the standard output (in any order).

*Hint*: use `java.security.MessageDigest` to compute SHA-256.

**How To Submit The Assignment**:
1. Accept the invitation link. Since you are reading this, this is likely done.
2. Just commit your code straight to the `master` branch.
3. Check that your code passes all the checks. Look for red/yellow/green dots in the "Commits" tab.
4. Once you have finished coding, fill the report below, inside `README.md`.
5. Check for any comments in the PR #1 (called "Feedback").

## Report

*Q*: How many threads do you use? Why?

*A*: 

I use one thread for each letter, because then all of these will execute in parallel. Not completely right I think because I have a i9-9900k processor with enabled hyperthreading. The processor has 8 cores so with hyperthreading it can launch 16 cores. Therefore I can launch 16 threads simultaneously. It will keep launching the new threads after one has finished untill the full alhpabet is processed.

*Q*: How do you use threads in your solution? Why?

*A*: 

I created a class called Concurrency which extends thread such that this class inherits all the properties of the thread class. I created a runnable inside to be able to execute the code inside on thread.start. This allows for creating the concurrency thread/class in for loop.

*Q*: Measure the time required to find the shortest alpha subtle string for every character `C`. Measure the total program execution time. Do these durations match? Why?

*A*: 

No the durations do not match at all. That is because the shortest alpha subtle string starting with that character C is different for each letter. Some take a lot more time to find than others (because the shortest string is a lot longer).

## Help

### To build
```
gradle build
```

### To run tests
```
gradle test
```

### To run a particular Java class
```
# From the main source set.
java -cp ./build/classes/java/main/ FullyQualifiedClassName
# From the test source set.
java -cp ./build/classes/java/test/ FullyQualifiedClassName
```

