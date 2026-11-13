package edu.jhuapl.trinity.javafx.javafx3d.contextgraph;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import org.junit.jupiter.api.Test;

/** Verify meaningful drill-down skips one-child Louvain levels without altering partitions. */
public final class ContextGraphCommunityDrilldownTest {
    private static ContextGraphCommunityDescriptor descriptor(int level, int id, int... children) {
        List<Integer> ids = new java.util.ArrayList<>();
        for (int child : children) ids.add(child);
        return new ContextGraphCommunityDescriptor(new ContextGraphCommunityRef(level, id),
            -1, ids, 10, "Source group", Map.of("Technique", 10), List.of());
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    @Test
    public void skipsConsecutiveSingleChildLevels() {
        Map<ContextGraphCommunityRef, ContextGraphCommunityDescriptor> records = new HashMap<>();
        for (var value : List.of(
            descriptor(3, 0, 0),
            descriptor(2, 0, 0),
            descriptor(1, 0, 0, 1),
            descriptor(0, 0), descriptor(0, 1))) records.put(value.ref(), value);
        var actual = ContextGraphCommunityDrilldown.visibleChildren(
            new ContextGraphCommunityRef(3, 0), records::get);
        check(actual.equals(List.of(new ContextGraphCommunityRef(0, 0),
            new ContextGraphCommunityRef(0, 1))), "Did not skip degenerate upper levels");
    }

    @Test
    public void preservesImmediateBranching() {
        Map<ContextGraphCommunityRef, ContextGraphCommunityDescriptor> records = new HashMap<>();
        var root = descriptor(3, 0, 1, 2);
        records.put(root.ref(), root);
        var actual = ContextGraphCommunityDrilldown.visibleChildren(root.ref(), records::get);
        check(actual.equals(List.of(new ContextGraphCommunityRef(2, 1),
            new ContextGraphCommunityRef(2, 2))), "Real immediate subdivision was skipped");
    }

    @Test
    public void leafAndUnknownHaveNoDerivedChildren() {
        var leaf = descriptor(0, 0);
        check(ContextGraphCommunityDrilldown.visibleChildren(leaf.ref(), r -> leaf).isEmpty(),
            "Original-entity leaf incorrectly contains derived child");
        check(ContextGraphCommunityDrilldown.visibleChildren(
            new ContextGraphCommunityRef(2, 9), ref -> null).isEmpty(),
            "Unknown community produced child");
    }

    public static void main(String[] args) {
        var test = new ContextGraphCommunityDrilldownTest();
        test.skipsConsecutiveSingleChildLevels();
        test.preservesImmediateBranching();
        test.leafAndUnknownHaveNoDerivedChildren();
        System.out.println("ContextGraphCommunityDrilldownTest: 3 checks PASSED");
    }
}
