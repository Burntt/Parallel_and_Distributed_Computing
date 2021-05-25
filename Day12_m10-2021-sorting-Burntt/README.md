# Sorting

## Statement

Parallel programming is not an easy discipline. Sometimes its even easier to go without any threading at all.

Try to implement a parallel version of a sorting algorithm, which runs faster than the single-threaded one.
And by *faster* I mean with a statistical significance.

You may try to go with the quick sort. Or with the merge sort.

*Hint*: For small inputs threading is an overkill.

Implement your sorting algorithm in `Sorting.java` and fill in the report below.

**How To Submit The Assignment**:
1. Accept the invitation link. Since you are reading this, this is likely done.
2. Just commit your code straight to the `master` branch.
3. Check that your code passes all the checks. Look for red/yellow/green dots in the "Commits" tab.
4. Once you have finished coding, fill the report below, inside `README.md`.
5. Check for any comments in the PR #1 (called "Feedback").

## Report

*Number of cores on your laptop*: 16
*Number of threads you use*: 2

Input Size  | `Arrays.sort` (mean time in ms) | Your algorithm (time in ms) | Who won?
------------|---------------------------------|-----------------------------|----------
  1 000 000 | 63.800ms +-   25.389ms          | 37.700ms +-   11.096ms      | ```yourSuperSortAlgorithm```! (p-value: 0.0056)
 10 000 000 | 655.600ms +-   10.522ms         | 389.100ms +-    7.141ms     | ```yourSuperSortAlgorithm```! (p-value: 0.0000)
100 000 000 | 7536.600ms +-  128.455ms        |  4307.800ms +-   47.286ms   | ```yourSuperSortAlgorithm```! (p-value: 0.0000)

## Help

### To run
```
gradle run
```

### To run a particular Java class
```
# From the main source set.
java -cp ./build/classes/java/main/ FullyQualifiedClassName
# From the test source set.
java -cp ./build/classes/java/test/ FullyQualifiedClassName
```

