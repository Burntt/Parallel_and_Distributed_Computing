# Search Engine

## Statement

This assignment covers various tricks that you could use to improve the performance of request-response-style applications.
Namely, you will be optimizing the search engine!

You high-level goal is to implement the `SearchEngine` class.

The search engine is tiered: there is a base layer and a middle layer.
The servers at the base layer search through the documents and return ranked relevant responses.
The server at the middle layer coordinates all the base servers.

The servers at the base layer are organized in the groups named "shards".
Each shard consists of several identical "replicas".

The purpose of the shards is to scale the number of documents avaliable to the search engine.
Each shard is responsible for searching only through its own part of the documents.

The purpose of the replicas is to scale the throughput of the system.
Each replica within a shard is equivalent (in terms of the searched documents) to any other replica within a shard.

Therefore, in order to serve a user request, the middle layer must issue concurrent subrequests
to each shard (to any replica within a shard), wait for the results, and merge them
(leave only the most relevant documents; if there are two documents with the same ID in the result -- keep the one with the highest relevance).

Start with the `SearchEngine` class -- which resembles the middle layer -- and implement it.
Check the tests in the `TestSearchEngine` class.

Once you get a correct implementation, your goal is to optimize the performance.

There is a benchmark -- `SearchEngineBenchmark` -- which simulates the load and reports the performance.
Precisely, it gathers request completion times and outputs statistics: mean request time, max request time, and quantiles.

You shall optimize the Q90 & Q99 quantiles.

Here is a list of ideas you may try implementing:
- issue requests from the middle layer to the shards in parallel;
- choose a random replica on each request;
- implement the hedged requests (if the replica is "too slow", then issue a parallel request to another replica hoping that it will serve the request faster than the primary replica).

JFYI, here are the numbers from my machine:
```
I [+   16793ms] T[main      ]: MIN:    3,069ms | MAX:  138,511ms | MEAN:    8,536ms +-   13,662ms
I [+   16796ms] T[main      ]: Q50:    3,579ms | Q90:   31,774ms |  Q99:   62,995ms
```

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

### How to benchmark the assignment without an IDE

```
gradle run
```

### How to run a particular Java class
```
# From the main source set.
java -cp ./build/classes/java/main/ FullyQualifiedClassName
# From the test source set.
java -cp ./build/classes/java/test/ FullyQualifiedClassName
```
