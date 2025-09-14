package edu.jhuapl.trinity.javafx.services;

import edu.jhuapl.trinity.data.messages.xai.FeatureVector;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static edu.jhuapl.trinity.javafx.services.FeatureVectorManagerService.SamplingMode;

final class DisplayListSampler {
    private DisplayListSampler() {}

    static List<FeatureVector> sample(List<FeatureVector> src, SamplingMode mode) {
        if (src == null) return Collections.emptyList();
        int n = src.size();
        if (n == 0 || mode == null || mode == SamplingMode.ALL) return src;

        int k = 1000;
        switch (mode) {
            case HEAD_1000: return src.subList(0, Math.min(k, n));
            case TAIL_1000: return src.subList(Math.max(0, n - k), n);
            case RANDOM_1000:
                if (n <= k) return src;
                ThreadLocalRandom rnd = ThreadLocalRandom.current();
                HashSet<Integer> idx = new HashSet<>(k * 2);
                while (idx.size() < k) idx.add(rnd.nextInt(n));
                ArrayList<FeatureVector> out = new ArrayList<>(k);
                for (Integer i : idx) out.add(src.get(i));
                return out;
            default:
                return src;
        }
    }
}
