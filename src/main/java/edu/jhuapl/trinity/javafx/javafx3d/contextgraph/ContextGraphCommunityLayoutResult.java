package edu.jhuapl.trinity.javafx.javafx3d.contextgraph;

/** Worker result combining a cached topology hierarchy with independently tunable geometry. */
record ContextGraphCommunityLayoutResult(ContextGraphCommunityHierarchy hierarchy,
                                         ContextGraphHyperbolicLayout.LayoutResult layout,
                                         ContextGraphSpatialDiagnostics diagnostics) { }
