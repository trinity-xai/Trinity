package edu.jhuapl.trinity.javafx.services;

import edu.jhuapl.trinity.data.messages.xai.FeatureVector;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/** Grab-bag of common helpers used by FeatureVector Manager UI & service. */
public final class FeatureVectorUtils {
    private FeatureVectorUtils() {}

    /* ---------------- String helpers ---------------- */

    /** null-safe: returns empty string when s==null */
    public static String nullToEmpty(String s) { return s == null ? "" : s; }

    /** Normalizes for case-insensitive contains. */
    public static String normalize(String s) { return s == null ? null : s.toLowerCase(Locale.ROOT).trim(); }

    /** Case-insensitive "contains" against any object (via toString()); q must be normalized already. */
    public static boolean containsIgnoreCase(Object value, String qNormalized) {
        if (value == null || qNormalized == null || qNormalized.isEmpty()) return false;
        String v = normalize(String.valueOf(value));
        return v != null && v.contains(qNormalized);
    }

    /** Trims trailing zeros from a decimal representation. */
    public static String trimDouble(double d) {
        String s = String.format(Locale.ROOT, "%.6f", d);
        if (s.contains(".")) s = s.replaceAll("0+$", "").replaceAll("\\.$", "");
        return s;
    }

    /** Safe filename mapping (Windows reserved chars, etc.). */
    public static String safeFilename(String s) {
        if (s == null) return "collection";
        return s.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    /** Normalize/clean a collection name (null-safe trim). */
    public static String cleanCollectionName(String s) {
        return (s == null) ? "" : s.trim();
    }

    /* ---------------- Vector cloning/copying ---------------- */

    /** Deep-ish clone of a FeatureVector (copies lists and metadata map). */
    public static FeatureVector cloneVector(FeatureVector src) {
        if (src == null) return null;
        FeatureVector fv = new FeatureVector();
        fv.setEntityId(src.getEntityId());
        fv.setLabel(src.getLabel());
        fv.setData(src.getData() == null ? null : new ArrayList<>(src.getData()));
        fv.setBbox(src.getBbox() == null ? null : new ArrayList<>(src.getBbox()));
        fv.setImageId(src.getImageId());
        fv.setFrameId(src.getFrameId());
        fv.setImageURL(src.getImageURL());
        fv.setMediaURL(src.getMediaURL());
        fv.setScore(src.getScore());
        fv.setPfa(src.getPfa());
        fv.setLayer(src.getLayer());
        fv.setText(src.getText());
        if (src.getMetaData() != null) {
            fv.setMetaData(new LinkedHashMap<>(src.getMetaData()));
        }
        return fv;
    }

    /** Copy a list of FeatureVectors via {@link #cloneVector(FeatureVector)}. */
    public static List<FeatureVector> copyVectors(List<FeatureVector> in) {
        if (in == null || in.isEmpty()) return List.of();
        ArrayList<FeatureVector> out = new ArrayList<>(in.size());
        for (FeatureVector fv : in) out.add(cloneVector(fv));
        return out;
    }

    /* ---------------- Preview/format helpers ---------------- */

    /** Small numeric preview for a vector’s data list. */
    public static String previewList(List<Double> data, int firstN) {
        if (data == null || data.isEmpty()) return "[]";
        int n = Math.min(firstN, data.size());
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < n; i++) {
            if (i > 0) sb.append(", ");
            Double v = data.get(i);
            if (v == null) sb.append("NaN");
            else {
                String s = String.format(Locale.ROOT, "%.6f", v);
                if (s.contains(".")) s = s.replaceAll("0+$", "").replaceAll("\\.$", "");
                sb.append(s);
            }
        }
        if (data.size() > n) sb.append(", …");
        sb.append("]");
        return sb.toString();
    }

    /** Pretty prints metadata as lines of "key: value". */
    public static String prettyMetadata(Map<String, String> md) {
        if (md == null || md.isEmpty()) return "(no metadata)";
        StringBuilder sb = new StringBuilder();
        md.forEach((k, v) -> sb.append(k == null ? "(null)" : k)
                               .append(": ")
                               .append(v == null ? "(null)" : v)
                               .append("\n"));
        // strip trailing newline
        int len = sb.length();
        return len > 0 ? sb.deleteCharAt(len - 1).toString() : "";
    }

    /** Parses simple `key=value` lines into a LinkedHashMap (preserves input order). */
    public static Map<String, String> parseKeyValues(String text) {
        Map<String, String> map = new LinkedHashMap<>();
        if (text == null || text.isBlank()) return map;
        String[] lines = text.split("\\R");
        for (String line : lines) {
            String ln = line.trim();
            if (ln.isEmpty()) continue;
            int eq = ln.indexOf('=');
            if (eq < 0) map.put(ln, "");
            else {
                String k = ln.substring(0, eq).trim();
                String v = ln.substring(eq + 1).trim();
                if (!k.isEmpty()) map.put(k, v);
            }
        }
        return map;
    }

    /* ---------------- Collection naming / merging ---------------- */

    /**
     * Derives a friendly collection name from a hint (filename/path or arbitrary string)
     * and/or falls back to label/size, then a UUID.
     */
    public static String deriveCollectionName(Object sourceHint, List<FeatureVector> fvs) {
        // 1) filename-ish
        if (sourceHint instanceof String s && !s.isBlank()) {
            String base = s.replace('\\', '/');
            int slash = base.lastIndexOf('/');
            if (slash >= 0) base = base.substring(slash + 1);
            int dot = base.lastIndexOf('.');
            if (dot > 0) base = base.substring(0, dot);
            if (!base.isBlank()) return base;
        }
        // 2) labels
        String label = (fvs != null && !fvs.isEmpty()) ? nullToEmpty(fvs.get(0).getLabel()) : "";
        if (!label.isBlank()) return label + " (" + (fvs == null ? 0 : fvs.size()) + ")";
        // 3) fallback
        return "Collection-" + UUID.randomUUID().toString().substring(0, 8);
    }

    /** Merge helper with optional dedupe by entityId (null IDs are always appended). */
    public static List<FeatureVector> mergeVectors(List<FeatureVector> target,
                                                   Collection<FeatureVector> source,
                                                   boolean dedupeByEntityId) {
        if (source == null || source.isEmpty()) return target;
        if (target == null) target = new ArrayList<>();

        if (!dedupeByEntityId) {
            target.addAll(source);
            return target;
        }
        Set<Object> seen = target.stream().map(FeatureVector::getEntityId).collect(Collectors.toSet());
        for (FeatureVector fv : source) {
            Object id = fv.getEntityId();
            if (id == null || !seen.contains(id)) {
                target.add(fv);
                if (id != null) seen.add(id);
            }
        }
        return target;
    }

    /* ---------------- Sampling & filtering ---------------- */

    /** Applies sampling policy used by the Manager. */
    public static List<FeatureVector> applySampling(List<FeatureVector> all,
                                                    FeatureVectorManagerService.SamplingMode mode) {
        if (all == null || all.isEmpty() || mode == null || mode == FeatureVectorManagerService.SamplingMode.ALL)
            return all == null ? List.of() : all;

        int n = all.size();
        int k = Math.min(1000, n);
        switch (mode) {
            case HEAD_1000:
                return all.subList(0, k);
            case TAIL_1000:
                return all.subList(n - k, n);
            case RANDOM_1000:
                if (n <= k) return all;
                ThreadLocalRandom rnd = ThreadLocalRandom.current();
                ArrayList<FeatureVector> copy = new ArrayList<>(all);
                for (int i = n - 1; i > 0; i--) {
                    int j = rnd.nextInt(i + 1);
                    var tmp = copy.get(i);
                    copy.set(i, copy.get(j));
                    copy.set(j, tmp);
                }
                return copy.subList(0, k);
            default:
                return all;
        }
    }

    /** Alias used by the service refactor; delegates to {@link #applySampling(List, FeatureVectorManagerService.SamplingMode)}. */
    public static List<FeatureVector> sample(List<FeatureVector> src, FeatureVectorManagerService.SamplingMode mode) {
        return applySampling(src, mode);
    }

    /** Returns true if any of label, text, or metadata values contains the normalized query. */
    public static boolean matchesTextFilter(FeatureVector fv, String normalizedQuery) {
        if (fv == null) return false;
        if (normalizedQuery == null || normalizedQuery.isEmpty()) return true;
        if (containsIgnoreCase(fv.getLabel(), normalizedQuery)) return true;
        if (containsIgnoreCase(fv.getText(), normalizedQuery)) return true;
        if (fv.getMetaData() != null) {
            for (var v : fv.getMetaData().values()) {
                if (containsIgnoreCase(v, normalizedQuery)) return true;
            }
        }
        return false;
    }

    /* ---------------- CSV export ---------------- */

    public static void writeCsv(File file, List<FeatureVector> src) throws IOException {
        int maxDim = 0;
        if (src != null) {
            for (FeatureVector fv : src) {
                if (fv != null && fv.getData() != null) {
                    maxDim = Math.max(maxDim, fv.getData().size());
                }
            }
        }
        try (BufferedWriter w = new BufferedWriter(new FileWriter(file))) {
            // header
            StringBuilder header = new StringBuilder("label,score,pfa,layer");
            for (int i = 0; i < maxDim; i++) header.append(",v").append(i);
            w.write(header.toString());
            w.newLine();
            // rows
            if (src != null) {
                for (FeatureVector fv : src) {
                    if (fv == null) continue;
                    StringBuilder row = new StringBuilder();
                    row.append(escapeCsv(fv.getLabel())).append(",")
                       .append(fv.getScore()).append(",")
                       .append(fv.getPfa()).append(",")
                       .append(fv.getLayer());
                    List<Double> data = fv.getData();
                    for (int i = 0; i < maxDim; i++) {
                        row.append(",");
                        if (data != null && i < data.size()) {
                            Double val = data.get(i);
                            if (val != null) row.append(val);
                        }
                    }
                    w.write(row.toString());
                    w.newLine();
                }
            }
        }
    }

    public static String escapeCsv(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
    public static String cleanName(String s) {
        if (s == null) return "";
        return s.trim();
    }    
}
