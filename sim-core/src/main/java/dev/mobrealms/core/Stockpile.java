package dev.mobrealms.core;

import java.util.*;

/** Integer accounting; no negative stocks and no silent overflow. */
public final class Stockpile {
    private final TreeMap<String, Long> items = new TreeMap<>();
    public long count(String item) { return items.getOrDefault(item, 0L); }
    public Map<String, Long> snapshot() { return Collections.unmodifiableMap(new TreeMap<>(items)); }
    public void add(String item, long count) {
        validate(item, count);
        items.put(item, Math.addExact(count(item), count));
    }
    public boolean take(String item, long count) {
        validate(item, count);
        long available = count(item);
        if (available < count) return false;
        if (available == count) items.remove(item); else items.put(item, available - count);
        return true;
    }
    public long transferTo(Stockpile target, String item, long requested) {
        Objects.requireNonNull(target); validate(item, requested);
        if (target == this) return 0;
        long amount = Math.min(count(item), requested);
        if (amount == 0) return 0;
        Math.addExact(target.count(item), amount); // preflight before either side is changed
        target.add(item, amount);
        take(item, amount);
        return amount;
    }
    private static void validate(String item, long count) {
        if (item == null || !item.matches("[a-z0-9_.-]+:[a-z0-9_./-]+") || count <= 0)
            throw new IllegalArgumentException("Invalid item/count");
    }
}
