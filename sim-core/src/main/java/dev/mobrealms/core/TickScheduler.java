package dev.mobrealms.core;

import java.util.ArrayDeque;
import java.util.Objects;
import java.util.function.LongSupplier;

/** Cooperative budget: one task can overrun; no new task starts after the deadline. */
public final class TickScheduler {
    private final ArrayDeque<Runnable> queue = new ArrayDeque<>();
    private final LongSupplier clock;
    private final int capacity;
    public TickScheduler(int capacity) { this(capacity, System::nanoTime); }
    public TickScheduler(int capacity, LongSupplier clock) {
        if (capacity < 1) throw new IllegalArgumentException("capacity");
        this.capacity = capacity; this.clock = Objects.requireNonNull(clock);
    }
    public boolean submit(Runnable task) {
        Objects.requireNonNull(task);
        if (queue.size() >= capacity) return false;
        queue.addLast(task); return true;
    }
    public int pending() { return queue.size(); }
    public int run(long budgetNanos, int maxTasks) {
        if (budgetNanos < 0 || maxTasks < 0) throw new IllegalArgumentException("budget");
        long start = clock.getAsLong(); int completed = 0;
        while (!queue.isEmpty() && completed < maxTasks && clock.getAsLong() - start < budgetNanos) {
            queue.removeFirst().run(); completed++;
        }
        return completed;
    }
}
