# FinView

## Statement

You are implementing a simple finance application that keeps track of the S&P 500.

The interface is rather simple:

```
+--------------------------------------------------------------------+
|                                                                    |
|   +------------------------------------------------------------+   |
|   | SPY: 3000.548; ticked 2ms ago; changed -4.15%              |   |
|   +------------------------------------------------------------+   |
|                                                                    |
|                                                                    |
|   +---------+                                                      |
|   | REFRESH |                                                      |
|   +---------+                                                      |
|                                                                    |
+--------------------------------------------------------------------+
```

There is just a text area to display some text and a refresh button.

You are targeting the mobile platform that comes with the vendored UI library `UILibrary`.
You briefly skimmed through the UI library documentation, and figured out that:
* The library provides the `UIEventListener` interface; you may implement it to intercept events such as clicks.
* The library requires the listeners to be very-fast so the interface will be responsive.
* The library requires just the one thread to render the interface.
* You have to register the renderer thread before invoking the `render` method.
* You have to make sure that renderer is fast as well so the interface will be responsive.

For more details -- see classes `UILibrary`, `UIEventListener`, `UIRenderTarget`.

As for the data, you have found a particular library on GitHub which you can use to provide the quotes.
However, the library -- `QuoteLibrary` -- is not that easy to use. Namely:
* The library requires all the methods to be executed from the one user thread.
* The user thread must be registered before invoking any methods.
* The library is able to provide only SPY quotes (which is OK).
* The library is able to provide only the latest quoted price.

Your end-goal is to fetch new quotes and update the text once the user clicks the "Refresh" button.
The text must be formatted as shown in the example ("SPY: XXXX.XXXX; ticked YYYms ago; changed +ZZ.ZZ%").

After spending some time designing the app architecture, you've decided to:
* isolate the quote library with a `QuoteProvider` interface, that hides all the details and provides a consistent API to use;
* write a glue-class `FinViewApplication` that connects the UI library and the quote provider.

*Complete the implementation of `QuoteProviderImpl` and `FinViewApplication` classes.*

*Hints*:
* Start with `QuoteProviderImpl`. Make sure that tests `TestQuoteProviderService` pass.
* Create a background thread inside the `QuoteProviderImpl` to ease working with the quote library.
* Complete the implementation of the `FinViewApplication`.
* Make sure you do not block any UI interactions.
* If you are struggling to make tests pass, read through the tests sources and use the debugger.

## Report

*Q*: Document any issues / challenges that you have faced during the assignment.

*A*: Struggled to understand the calls to a function that should have been implemented, because I am used so far there would be the function name and the TODO. Therefore, I was confused about this particular assignment.

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
