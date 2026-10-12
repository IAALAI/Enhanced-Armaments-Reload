package nova.committee.enhancedarmaments.util;

import net.minecraft.util.RandomSource;

import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;

public class RandomCollection<E> {
    private final NavigableMap<Double, E> map = new TreeMap<>();
    private double total = 0;

    public void add(double weight, E result) {
        double nextTotal = total + weight;
        if (!Double.isFinite(weight) || weight <= 0 || !Double.isFinite(nextTotal)) {
            return;
        }
        total = nextTotal;
        map.put(total, result);
    }

    public Optional<E> next(RandomSource random) {
        if (map.isEmpty() || !Double.isFinite(total) || total <= 0D) {
            return Optional.empty();
        }
        double value = random.nextDouble() * total;
        var entry = map.ceilingEntry(value);
        if (entry == null) {
            entry = map.lastEntry();
        }
        return entry == null ? Optional.empty() : Optional.ofNullable(entry.getValue());
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public void clear() {
        map.clear();
        total = 0;
    }
}
