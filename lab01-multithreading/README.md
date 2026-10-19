# Multithreading Lab - Task 2: Fixing a race condition

## Question:
Why does making only `increment()` synchronized but not `get()` still leave a bug?

### Why it leaves a bug and what breaks:
1. **Memory Visibility (Java Memory Model / JMM)**: 
   Even though `increment()` is synchronized (which safely flushes writes to main memory when the lock is released), `get()` is not synchronized. Without synchronization or volatile semantics on `get()`, the reading thread might read a stale cached value from its local CPU cache or registers rather than fetching the latest updated value from main memory.
2. **Race Conditions & Consistency During Reads**: 
   Because `get()` lacks synchronization, a thread calling it might read the counter value while another thread is mid-operation, or it might observe inconsistent visibility lags across multi-core processors. 

### Result:
Even if updates are safely serialized inside `increment()`, calling an unsynchronized `get()` can return outdated, incorrect, or inconsistent results because the Java Memory Model does not guarantee that the reading thread will see the latest memory updates without a corresponding synchronization barrier or volatile read.

# Multithreading Lab - Task 3: Bounded Buffer Experiment

## Experiment: Replacing `while` with `if` and `notifyAll()` with `notify()`

If you change the condition check from a `while` loop to an `if` statement and replace `notifyAll()` with `notify()`:

### What Breaks and Why:
1. **Signal Theft / Lost Wakeups (`notify()` instead of `notifyAll()`)**: 
   `notify()` wakes up only *a single* arbitrary thread waiting on the object's monitor. If multiple producer and consumer threads are waiting, `notify()` might wake up a thread of the *same* type (e.g., a producer waking another producer when space becomes available, instead of a consumer). That thread sees the condition is still invalid and goes back to sleep, causing a lost notification.
2. **Spurious Wakeups & Race Conditions (`if` instead of `while`)**:
   An `if` statement checks the condition only once when the thread is first awakened. If multiple threads wake up or another thread modifies the buffer state before the awakened thread acquires the lock, the condition might no longer hold true. Using `while` ensures the condition is re-evaluated every time the thread wakes up.

### Result:
Running this in a loop 20 times will frequently cause **deadlocks** or hang indefinitely because threads miss notifications or proceed under invalid states.