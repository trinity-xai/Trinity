package edu.jhuapl.trinity.javafx.javafx3d.contextgraph;

import org.junit.jupiter.api.Test;

/** Regression checks: summarizing edges must not summarize or hide source nodes. */
public final class ContextGraphCommunityDisclosurePolicyTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    @Test
    public void allSourceNodesRemainVisibleWithoutExplicitNodeFilters() {
        for (String id : java.util.Set.of("a", "b", "c"))
            check(ContextGraphCommunityDisclosurePolicy.originalVisible(true, false, true, false),
                "Source entity unexpectedly hidden: " + id);
    }

    @Test
    public void factorLabelVisibilityStillApplies() {
        check(!ContextGraphCommunityDisclosurePolicy.originalVisible(false, false, true, false),
            "Hidden category should remain hidden");
        check(ContextGraphCommunityDisclosurePolicy.originalVisible(true, false, true, false),
            "Visible category should remain visible");
    }

    @Test
    public void explicitlyIsolatedNeighborhoodStillApplies() {
        check(!ContextGraphCommunityDisclosurePolicy.originalVisible(true, true, false, false),
            "Outside isolated neighborhood leaked");
        check(ContextGraphCommunityDisclosurePolicy.originalVisible(true, true, false, true),
            "Neighborhood member was hidden");
        check(ContextGraphCommunityDisclosurePolicy.originalVisible(true, true, true, false),
            "Empty neighborhood must not hide all entities");
    }

    @Test
    public void edgeModesAreNotInputsToNodeVisibility() {
        // The policy intentionally has no edge display mode or community-level parameter.
        // A summarized connection can be revealed without changing source-node visibility.
        for (var mode : ContextGraphEdgeDisplayMode.values()) {
            check(ContextGraphCommunityDisclosurePolicy.originalVisible(true, false, false, false),
                "Edge mode " + mode + " must not affect node visibility");
        }
    }

    public static void main(String[] args) {
        var tests = new ContextGraphCommunityDisclosurePolicyTest();
        tests.allSourceNodesRemainVisibleWithoutExplicitNodeFilters();
        tests.factorLabelVisibilityStillApplies();
        tests.explicitlyIsolatedNeighborhoodStillApplies();
        tests.edgeModesAreNotInputsToNodeVisibility();
        System.out.println("ContextGraphCommunityDisclosurePolicyTest: 4 checks PASSED");
    }
}
