package edu.jhuapl.trinity.utils.statistics;

import edu.jhuapl.trinity.data.messages.xai.FeatureVector;

import java.io.Serial;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * DensityCache
 * ------------
 * LRU (+ optional TTL) cache for joint PDF/CDF grids produced by GridDensity3DEngine.
 * <p>
 * Cache key = datasetSignature ⨉ xAxis ⨉ yAxis ⨉ gridSpec (bins + bounds).
 * The datasetSignature can be provided by the caller (stable ID) or derived
 * from the data (fast fingerprint over a sampled subset).
 * <p>
 * Thread-safe via RW-lock; LRU eviction via access-ordered LinkedHashMap.
 * <p>
 * Typical usage:
 * DensityCache cache = new DensityCache.Builder().maxEntries(128).ttlMillis(0).build();
 * // build a canonical grid first (recommended)
 * GridSpec grid = CanonicalGridPolicy.get("default").gridForAxes(vectors, xAxis, yAxis, null, null, "myDataset");
 * GridDensityResult res = cache.getOrCompute(vectors, xAxis, yAxis, grid, "myDataset"); // provenance optional
 * <p>
 * Notes:
 * - GridDensityResult holds both PDF and CDF; one cache entry covers both.
 * - To ensure stable keys across runs, pass a true datasetId (e.g., content hash).
 * If you can't, the internal fingerprint is deterministic but approximate.
 * - Avoid caching empty datasets (we skip and return compute directly).
 *
 * @author Sean Phillips
 */
public final class DensityCache implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Cache statistics snapshot.
     */
    public static final class Stats implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        public final long hits, misses, puts, evictions, expirations, size;

        Stats(long hits, long misses, long puts, long evictions, long expirations, long size) {
            this.hits = hits;
            this.misses = misses;
            this.puts = puts;
            this.evictions = evictions;
            this.expirations = expirations;
            this.size = size;
        }

        @Override
        public String toString() {
            return "Stats{hits=" + hits + ", misses=" + misses + ", puts=" + puts +
                ", evictions=" + evictions + ", expirations=" + expirations +
                ", size=" + size + '}';
        }
    }

    /**
     * Builder for DensityCache.
     */
    public static final class Builder {
        private int maxEntries = 128;
        private long ttlMillis = 0; // 0 = no TTL

        public Builder maxEntries(int n) {
            this.maxEntries = Math.max(1, n);
            return this;
        }

        public Builder ttlMillis(long ms) {
            this.ttlMillis = Math.max(0, ms);
            return this;
        }

        public DensityCache build() {
            return new DensityCache(maxEntries, ttlMillis);
        }
    }

    /**
     * Internal entry.
     */
    private static final class Entry implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        final GridDensityResult result;
        final long createdAt;
        final JpdfProvenance provenance; // optional; may be null

        Entry(GridDensityResult r, long t, JpdfProvenance p) {
            this.result = r;
            this.createdAt = t;
            this.provenance = p;
        }
    }

    private final int maxEntries;
    private final long ttlMillis;

    /**
     * Access-ordered LRU map with bounded size (evicts eldest).
     */
    private final LinkedHashMap<String, Entry> map;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    // Stats (best-effort; not atomic across fields)
    private long hits = 0, misses = 0, puts = 0, evictions = 0, expirations = 0;

    private DensityCache(int maxEntries, long ttlMillis) {
        this.maxEntries = maxEntries;
        this.ttlMillis = ttlMillis;
        this.map = new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Entry> eldest) {
                boolean evict = size() > DensityCache.this.maxEntries;
                if (evict) evictions++;
                return evict;
            }
        };
    }

    // =====================================================================================
    // Core cache operations
    // =====================================================================================

    /**
     * Get (or compute+insert) a GridDensityResult for the given inputs, storing the provided provenance.
     *
     * @param vectors    dataset rows; if null/empty, computes directly without caching
     * @param xAxis      axis params for X
     * @param yAxis      axis params for Y
     * @param grid       grid spec (bins & bounds should be explicit for canonical comparability)
     * @param datasetId  optional stable dataset identifier; if null, a fast fingerprint is derived
     * @param provenance optional provenance record to store alongside the result
     */
    public GridDensityResult getOrCompute(
        List<FeatureVector> vectors,
        AxisParams xAxis,
        AxisParams yAxis,
        GridSpec grid,
        String datasetId,
        JpdfProvenance provenance
    ) {
        Objects.requireNonNull(xAxis, "xAxis");
        Objects.requireNonNull(yAxis, "yAxis");
        Objects.requireNonNull(grid, "grid");

        if (vectors == null || vectors.isEmpty()) {
            // Nothing worth caching; delegate to engine
            return GridDensity3DEngine.computePdfCdf2D(vectors, xAxis, yAxis, grid);
        }

        final String key = makeKey(vectors, xAxis, yAxis, grid, datasetId);
        final long now = System.currentTimeMillis();

        // Fast path: read lock then compute if miss
        Entry e;
        lock.readLock().lock();
        try {
            e = map.get(key);
            if (e != null && !isExpired(e, now)) {
                hits++;
                return e.result;
            }
        } finally {
            lock.readLock().unlock();
        }

        // Compute outside lock
        GridDensityResult computed = GridDensity3DEngine.computePdfCdf2D(vectors, xAxis, yAxis, grid);

        // Write back if still missing (double-check)
        lock.writeLock().lock();
        try {
            Entry existing = map.get(key);
            if (existing != null && !isExpired(existing, now)) {
                hits++;
                return existing.result;
            }
            misses++;
            if (existing != null && isExpired(existing, now)) expirations++;
            map.put(key, new Entry(computed, now, provenance));
            puts++;
        } finally {
            lock.writeLock().unlock();
        }
        return computed;
    }

    /**
     * Overload: get (or compute+insert) without providing a provenance.
     * The cache will store a {@code null} provenance for this entry.
     */
    public GridDensityResult getOrCompute(
        List<FeatureVector> vectors,
        AxisParams xAxis,
        AxisParams yAxis,
        GridSpec grid,
        String datasetId
    ) {
        return getOrCompute(vectors, xAxis, yAxis, grid, datasetId, null);
    }

    /**
     * Manual insert (replaces existing).
     */
    public void put(String key, GridDensityResult value, JpdfProvenance provenance) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");
        lock.writeLock().lock();
        try {
            map.put(key, new Entry(value, System.currentTimeMillis(), provenance));
            puts++;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Overload: manual insert without provenance.
     */
    public void put(String key, GridDensityResult value) {
        put(key, value, null);
    }

    /**
     * Direct lookup by precomputed key; returns null if missing/expired.
     */
    public GridDensityResult get(String key) {
        lock.writeLock().lock(); // write: we may remove expired
        try {
            Entry e = map.get(key);
            if (e == null) return null;
            if (isExpired(e, System.currentTimeMillis())) {
                map.remove(key);
                expirations++;
                return null;
            }
            hits++;
            return e.result;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Return provenance for a cached entry, or null.
     */
    public JpdfProvenance getProvenance(String key) {
        lock.readLock().lock();
        try {
            Entry e = map.get(key);
            if (e == null) return null;
            return e.provenance;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Remove a specific key.
     */
    public void invalidate(String key) {
        lock.writeLock().lock();
        try {
            map.remove(key);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Clear all entries.
     */
    public void clear() {
        lock.writeLock().lock();
        try {
            map.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Snapshot of stats.
     */
    public Stats stats() {
        lock.readLock().lock();
        try {
            return new Stats(hits, misses, puts, evictions, expirations, map.size());
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Current number of entries.
     */
    public int size() {
        lock.readLock().lock();
        try {
            return map.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    // =====================================================================================
    // Keying / fingerprints
    // =====================================================================================

    /**
     * Make a stable cache key for the inputs. If datasetId is null, a deterministic
     * fingerprint is derived by sampling up to SAMPLE_ROWS rows and a few columns.
     */
    public String makeKey(List<FeatureVector> vectors,
                          AxisParams xAxis,
                          AxisParams yAxis,
                          GridSpec grid,
                          String datasetId) {
        String ds = (datasetId != null && !datasetId.isBlank())
            ? datasetId
            : fingerprintDataset(vectors, 256, 64);
        return "ds=" + ds +
            "|x=" + axisKey(xAxis) +
            "|y=" + axisKey(yAxis) +
            "|g=" + gridKey(grid);
    }

    /**
     * Fast, deterministic fingerprint of the dataset (not cryptographic).
     */
    public static String fingerprintDataset(List<FeatureVector> vectors, int maxRows, int maxCols) {
        if (vectors == null || vectors.isEmpty()) return "empty";
        int n = vectors.size();
        int d = (vectors.get(0).getData() == null) ? 0 : vectors.get(0).getData().size();

        int rows = Math.min(n, Math.max(1, maxRows));
        int cols = Math.min(d, Math.max(1, maxCols));

        long h = 1469598103934665603L; // FNV-ish seed
        long p = 1099511628211L;

        int stepR = Math.max(1, n / rows);
        int stepC = Math.max(1, d / cols);

        for (int r = 0, countR = 0; r < n && countR < rows; r += stepR, countR++) {
            List<Double> row = vectors.get(r).getData();
            for (int c = 0, countC = 0; c < d && countC < cols; c += stepC, countC++) {
                double v = (c < row.size()) ? row.get(c) : 0.0;
                long bits = Double.doubleToLongBits(v);
                h ^= bits;
                h *= p;
            }
            // include some metadata (size)
            h ^= row.size();
            h *= p;
        }
        h ^= n * 31L + d;
        return Long.toUnsignedString(h, 36); // compact base36
    }

    private static String axisKey(AxisParams a) {
        StringBuilder sb = new StringBuilder(64);
        sb.append(a.getType());
        if (a.getType() == StatisticEngine.ScalarType.METRIC_DISTANCE_TO_MEAN) {
            sb.append(":metric=").append(String.valueOf(a.getMetricName()));
            sb.append(":ref=").append(vecHash(a.getReferenceVec()));
        } else if (a.getType() == StatisticEngine.ScalarType.COMPONENT_AT_DIMENSION) {
            sb.append(":comp=").append(String.valueOf(a.getComponentIndex()));
        }
        String lbl = a.getMetricName();
        if (lbl != null && !lbl.isBlank()) sb.append(":lbl=").append(lbl);
        return sb.toString();
    }

    private static String gridKey(GridSpec g) {
        // Bounds may be null -> treat as 'auto'. Prefer explicit bounds for canonical comparability.
        return "bx=" + g.getBinsX() + ",by=" + g.getBinsY() +
            ",minx=" + (g.getMinX() == null ? "auto" : g.getMinX()) +
            ",maxx=" + (g.getMaxX() == null ? "auto" : g.getMaxX()) +
            ",miny=" + (g.getMinY() == null ? "auto" : g.getMinY()) +
            ",maxy=" + (g.getMaxY() == null ? "auto" : g.getMaxY());
    }

    private static String vecHash(List<Double> v) {
        if (v == null || v.isEmpty()) return "null";
        long h = 0xcbf29ce484222325L, p = 0x100000001b3L;
        int step = Math.max(1, v.size() / 16); // sample up to 16 entries
        for (int i = 0; i < v.size(); i += step) {
            long bits = Double.doubleToLongBits(v.get(i));
            h ^= bits;
            h *= p;
        }
        h ^= v.size();
        return Long.toUnsignedString(h, 36);
    }

    private boolean isExpired(Entry e, long now) {
        if (ttlMillis <= 0) return false;
        return (now - e.createdAt) > ttlMillis;
    }
}
