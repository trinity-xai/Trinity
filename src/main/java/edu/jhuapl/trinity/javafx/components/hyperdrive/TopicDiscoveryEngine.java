package edu.jhuapl.trinity.javafx.components.hyperdrive;

import com.clust4j.algo.*;
import edu.jhuapl.trinity.utils.clustering.ClusterMethod;
import edu.jhuapl.trinity.utils.clustering.GaussianMixtureModel;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Topic discovery operates on the ORIGINAL N-dimensional embeddings, not 3D projections.
 * No JavaFX dependencies: safe for a worker thread and deterministic regression tests.
 */
public final class TopicDiscoveryEngine {
    private TopicDiscoveryEngine() { }

    public record Sample(int id, String text, double[] vector) { }
    public record Options(ClusterMethod method, int clusters, int minimumClusterSize,
                          int minimumPoints, double epsilon, int maxIterations) { }
    public record Result(List<Topic> topics, int noiseCount, int sampleCount) { }

    public static final class Topic {
        private final int id;
        private final List<Sample> members;
        private final double[] centroid;
        private final List<String> representative;
        private List<String> keywords;
        private boolean autoNamed = true;
        private final double cohesion;
        private double distinctiveness = Double.NaN;
        private String name;
        private String summary;
        private boolean included = true;

        private Topic(int id, List<Sample> members, int maxRepresentatives) {
            this.id = id;
            this.members = List.copyOf(members);
            int dim = members.get(0).vector().length;
            this.centroid = new double[dim];
            // Preserve the source embedding scale for the existing metric selector,
            // including Euclidean and other magnitude-sensitive distances.
            for (Sample s : members) {
                for (int d = 0; d < dim; d++) centroid[d] += s.vector()[d] / members.size();
            }
            double score = 0;
            List<Sample> sorted = new ArrayList<>(members);
            sorted.sort(Comparator.comparingDouble(s -> -cosine(s.vector(), centroid)));
            for (Sample sample : members) score += cosine(sample.vector(), centroid);
            cohesion = score / members.size();
            representative = sorted.stream().map(Sample::text).filter(Objects::nonNull)
                .filter(s -> !s.isBlank()).limit(maxRepresentatives).map(s -> truncate(s, 800)).toList();
            keywords = List.of();
            name = "Topic " + (id + 1);
            summary = "";
        }
        public int id() { return id; }
        public List<Sample> members() { return members; }
        public int size() { return members.size(); }
        public double[] centroid() { return centroid.clone(); }
        public List<String> representative() { return representative; }
        public List<String> keywords() { return keywords; }
        public double cohesion() { return cohesion; }
        public double distinctiveness() { return distinctiveness; }
        public String name() { return name; }
        public void setName(String name) {
            if (name != null && !name.isBlank()) {
                this.name = name.strip();
                this.autoNamed = false;
            }
        }
        public String summary() { return summary; }
        public void setSummary(String summary) { this.summary = summary == null ? "" : summary.strip(); }
        public boolean included() { return included; }
        public void setIncluded(boolean included) { this.included = included; }
        @Override public String toString() { return (included ? "[x] " : "[ ] ") + name + " (" + size() + ")"; }
    }

    /** Vectors should already be finite and of identical dimensions. */
    public static Result discover(List<Sample> samples, Options options) {
        if (samples == null || samples.size() < 2) throw new IllegalArgumentException("At least two embedded text chunks are required.");
        if (options == null || options.method() == null) throw new IllegalArgumentException("Select a clustering method.");
        int dim = samples.get(0).vector().length;
        double[][] observations = new double[samples.size()][dim];
        for (int i = 0; i < samples.size(); i++) {
            double[] v = samples.get(i).vector();
            if (!TextLandmarkSimilarityEngine.usable(v) || v.length != dim)
                throw new IllegalArgumentException("All topic discovery embeddings must be finite and have matching dimensions.");
            observations[i] = v.clone();
            if (!normalize(observations[i])) throw new IllegalArgumentException("Zero vectors cannot be clustered with normalized distance.");
        }
        int[] labels = fit(observations, options);
        if (labels.length != samples.size()) throw new IllegalStateException("Clustering returned an incompatible number of assignments.");
        Map<Integer, List<Sample>> grouped = new TreeMap<>();
        int noise = 0;
        for (int i = 0; i < labels.length; i++) {
            if (labels[i] < 0) { noise++; continue; }
            // Store original vectors for numeric landmark prototypes; only the
            // clustering algorithm operates on the normalized working copy.
            grouped.computeIfAbsent(labels[i], unused -> new ArrayList<>())
                .add(new Sample(samples.get(i).id(), samples.get(i).text(), samples.get(i).vector().clone()));
        }
        List<Topic> topics = new ArrayList<>();
        int idx = 0;
        for (List<Sample> group : grouped.values()) topics.add(new Topic(idx++, group, 5));
        topics.sort(Comparator.comparingInt(Topic::size).reversed());
        scoreDistinctiveness(topics);
        return new Result(List.copyOf(topics), noise, samples.size());
    }

    public static Topic merge(int id, Collection<Topic> topics) {
        List<Sample> merged = topics.stream().flatMap(t -> t.members().stream())
            .collect(Collectors.toMap(Sample::id, s -> s, (a, b) -> a, LinkedHashMap::new))
            .values().stream().toList();
        if (merged.isEmpty()) throw new IllegalArgumentException("Select topics to merge.");
        return new Topic(id, merged, 5);
    }

    /** Distance from each centroid to its nearest competing centroid in cosine space. */
    public static void scoreDistinctiveness(List<Topic> topics) {
        // Refresh contrastive topic terms when clusters are discovered or merged.
        refreshTopicKeywords(topics);
        for (Topic topic : topics) {
            double nearestSimilarity = -1;
            for (Topic other : topics) {
                if (topic == other) continue;
                nearestSimilarity = Math.max(nearestSimilarity, cosine(topic.centroid, other.centroid));
            }
            topic.distinctiveness = 1 - nearestSimilarity;
        }
    }

    private static int[] fit(double[][] samples, Options options) {
        var data = new Array2DRowRealMatrix(samples);
        int k = Math.max(1, Math.min(options.clusters(), samples.length));
        int iterations = Math.max(10, options.maxIterations());
        return switch (options.method()) {
            case KMEANS -> new KMeansParameters(k).setMaxIter(iterations).setVerbose(false)
                .fitNewModel(data).getLabels();
            case KMEDIODS -> new KMedoidsParameters(k).setMaxIter(iterations).setVerbose(false)
                .fitNewModel(data).getLabels();
            case DBSCAN -> new DBSCANParameters().setEps(options.epsilon())
                .setMinPts(Math.max(2, options.minimumPoints())).setVerbose(false)
                .fitNewModel(data).getLabels();
            case HDDBSCAN -> new HDBSCANParameters().setMinClustSize(Math.max(2, options.minimumClusterSize()))
                .setMinPts(Math.max(2, options.minimumPoints())).setVerbose(false)
                .fitNewModel(data).getLabels();
            case AFFINITY -> new AffinityPropagationParameters().setMaxIter(iterations)
                .setDampingFactor(0.7).setVerbose(false).fitNewModel(data).getLabels();
            case EX_MAX -> {
                GaussianMixtureModel gmm = GaussianMixtureModel.fit(k, samples, true);
                int[] groups = new int[samples.length];
                for (int i = 0; i < groups.length; i++) groups[i] = gmm.maxPostProb(samples[i]).getKey();
                yield groups;
            }
        };
    }

    private static boolean normalize(double[] vector) {
        double max = 0;
        for (double x : vector) max = Math.max(max, Math.abs(x));
        if (max == 0) return false;
        double sum = 0;
        for (double x : vector) sum += (x / max) * (x / max);
        double norm = max * Math.sqrt(sum);
        for (int i = 0; i < vector.length; i++) vector[i] /= norm;
        return true;
    }

    private static double cosine(double[] a, double[] b) {
        double dot = 0, aa = 0, bb = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            aa += a[i] * a[i];
            bb += b[i] * b[i];
        }
        return aa == 0 || bb == 0 ? 0 : dot / Math.sqrt(aa * bb);
    }

    private static final Set<String> STOP = Set.of("the", "and", "for", "with", "this", "that", "from", "are", "was", "were", "have", "has", "had", "into", "out", "not", "but", "its", "their", "then", "than", "about", "can", "could", "would", "should", "will", "being", "been", "which", "when", "where", "there", "these", "those", "they", "them", "you", "your", "our", "also", "using", "used", "such", "all", "one", "two", "per", "each", "any", "may", "more", "most", "other", "some");
    /** Class-based contrastive TF-IDF: distinguish a term in this topic from all other topics. */
    private static void refreshTopicKeywords(List<Topic> topics) {
        if (topics.isEmpty()) return;
        List<Map<String, Integer>> counts = new ArrayList<>(topics.size());
        Map<String, Integer> documentFrequency = new HashMap<>();
        for (Topic topic : topics) {
            Map<String, Integer> groupCounts = new HashMap<>();
            for (Sample sample : topic.members) {
                if (sample.text() == null) continue;
                String text = sample.text().length() > 8000 ? sample.text().substring(0, 8000) : sample.text();
                for (String word : text.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+")) {
                    if (word.length() >= 4 && !STOP.contains(word)) groupCounts.merge(word, 1, Integer::sum);
                }
            }
            counts.add(groupCounts);
            for (String word : groupCounts.keySet()) documentFrequency.merge(word, 1, Integer::sum);
        }
        for (int i = 0; i < topics.size(); i++) {
            Topic topic = topics.get(i);
            Map<String, Integer> terms = counts.get(i);
            long total = terms.values().stream().mapToLong(Integer::longValue).sum();
            List<String> ranked = terms.entrySet().stream()
                .sorted((a, b) -> {
                    int scoreOrder = Double.compare(
                        topicTermScore(b.getValue(), total, documentFrequency.get(b.getKey()), topics.size()),
                        topicTermScore(a.getValue(), total, documentFrequency.get(a.getKey()), topics.size()));
                    return scoreOrder != 0 ? scoreOrder : a.getKey().compareTo(b.getKey());
                }).limit(8).map(Map.Entry::getKey).toList();
            topic.keywords = ranked;
            if (topic.autoNamed) {
                topic.name = "Topic " + (topic.id + 1)
                    + (ranked.isEmpty() ? "" : ": " + String.join(", ", ranked.subList(0, Math.min(2, ranked.size()))));
            }
        }
    }

    private static double topicTermScore(int frequency, long total, int topicFrequency, int topicCount) {
        if (total == 0) return 0;
        // Shared schema tokens occurring in every topic receive near-zero weight.
        double idf = Math.log((topicCount + 0.5) / (topicFrequency + 0.5)) + 0.02;
        return frequency / (double) total * idf * idf;
    }
    private static String truncate(String s, int max) { return s.length() <= max ? s : s.substring(0, max) + "..."; }
}
