# Context Graph 3D — Developer Context and Implementation Guide

**Project:** Trinity XAI — Java / JavaFX (current subsystem guidance targets Java 25; confirm toolchain in checkout)  
**Status:** Living architectural and implementation context, updated 2026-11-06  
**Audience:** Human contributors and coding agents  
**Scope:** Dedicated Context Graph 3D viewer, graph model, layouts, rendering, controls, camera, interactions, performance and planned improvements.

> This document records accepted behavior and measured results, not a claim that every source snapshot is current. Always inspect the latest checkout and preserve user-verified changes. Proposed items are explicitly labeled.

## 1. Mission and scope

Context Graph 3D is Trinity's dedicated interactive 3D graph projection for exploring relationships and multiscale contextual hierarchies, including semantic connections between document chunks. It is a distinct `SubScene` projection rather than a feature embedded in Hypersurface. Graph structure is represented by `GraphDirectedCollection`, `GraphNode` and `GraphEdge`; visualization and interaction must not become the authoritative graph data model.

Longer-term use cases include hierarchical document/page/paragraph/sentence relationships, semantic links, community structures, and layouts that help analysts discover structure without knowing keywords in advance. These are goals, not all verified end-user features.

## 2. System architecture and important classes

Paths are relative to `src/main/java/edu/jhuapl/trinity/`; verify locations in the current checkout.

| Class / area | Responsibility |
|---|---|
| `javafx/javafx3d/contextgraph/ContextGraph3DPane.java` | SubScene, camera hierarchy, mouse/keyboard navigation, context menu, Fit Graph, pulse diagnostics |
| `javafx/javafx3d/contextgraph/ContextGraphController.java` | Rendering layers, graph loading, background preparation, progressive attachment, style/material state, layout and coordinate updates |
| `javafx/javafx3d/contextgraph/ContextGraphEdge3D.java` | Dedicated edge `MeshView`, shared unit-cylinder mesh, per-edge orientation/position/size |
| `javafx/javafx3d/contextgraph/ContextGraphInteractionController.java` | Node/edge interaction event routing and graph-related analytical signals |
| `javafx/components/panes/ContextGraphControlsPane.java` | Floating `LitPathPane` with Graph Layout, Graph Style and Display tabs |
| `javafx/components/GraphStyleControlsView.java` | Node/edge global color, radius/width and opacity controls; `GraphEvent` synchronization |
| `javafx/components/GraphControlsView.java` | Layout algorithm and parameter controls |
| `data/graph/GraphDirectedCollection.java`, `GraphNode.java`, `GraphEdge.java` | Graph model and stable node/edge identities |
| `data/graph/GraphFeatureCollectionAdapter.java` | Graph/FeatureCollection integration |
| `utils/graph/GraphLayoutParams.java`, `GraphStyleParams.java` | Layout/style parameters |
| `utils/graph/ForceFrLayout3D.java`, `GraphNetworkLayouts3D.java`, `GraphTopologyLayouts3D.java` | Layout computations |
| `javafx/events/GraphEvent.java`, `CommandTerminalEvent.java` | Graph control and notification events |
| `javafx/javafx3d/animated/AnimatedSphere.java` | Existing general-purpose interactive `Sphere` used by Context Graph nodes and other projections |
| `javafx/javafx3d/Projections3DPane.java` | Separate projection with heavy `AnimatedSphere` use; compatibility-sensitive |

`ContextGraphController` owns `graphLayer`, `nodeLayer`, `edgeLayer`, typed `nodeViews` and `edgeViews` registries, `layoutPositions`, `coordinateScale`, `styleParams`, and render/layout generation counters. Never use `Node.userData` for model-to-view mapping. A `ContextGraphNode3D` class is **planned**, not part of the last verified baseline.

## 3. Loading and rendering pipeline

1. Import/deserialize `GraphDirectedCollection` away from the FX thread where possible.
2. `loadGraph()` establishes current graph and starts `rebuildGraph()`.
3. A background JavaFX `Task` prepares detached node and edge objects and computes positions. Constructing `TriangleMesh` off the FX thread is accepted in this codebase.
4. Attach nodes to the scene graph on the FX thread. The established `Platform.runLater()` pattern is intentional; do not replace it based on an unmeasured assumption that many callbacks are inherently problematic.
5. Prepare all edges off-thread and attach them progressively using an FX-thread `AnimationTimer`, reading a configurable batch size on each callback. **Verified default: 100 edges per pulse.**
6. Use `renderGeneration` checks so stale loads cannot attach geometry after a replacement load. Keep layout-generation checks for stale layout results.
7. Preserve progress-indicator animation and camera responsiveness; never move expensive geometry preparation or layout computation back onto the FX thread.

### 3.1 Shared edge mesh — accepted optimization

`ContextGraphEdge3D` extends `MeshView`, not `Cylinder`. Edge instances share a cached unit-cylinder `TriangleMesh` (potentially keyed by radial divisions), but retain separate `MeshView` instances, transforms and materials. The unit cylinder is Y-aligned. Per-edge dimensions use local `Scale` (X/Z radius, Y length), a `Rotate` to align Y to the endpoint direction, and midpoint translation. **Transform ordering is essential:** scale in local cylinder coordinates before rotation; the wrong order previously produced vertical, parallel, distorted edges. The corrected order is user-verified.

The earlier restriction against Scale transforms was explicitly relaxed for solid-color Context Graph geometry, where texture distortion is not a concern. Do not assume the relaxation applies to textured meshes elsewhere.

**Mesh sharing and material sharing are separate choices.** Reusing one mesh does not require reusing one mutable material. Dynamic per-object coloration needs careful material ownership or a palette cache.

### 3.2 Node rendering — current and planned

Current Context Graph nodes use `AnimatedSphere extends Sphere`, usually with hover animation disabled during graph construction. `AnimatedSphere` also supports selection, radius changes, hover `ScaleTransition`, and texture drag/drop. It is used extensively in `Projections3DPane`, where individual colors and labels can change at runtime.

**Next agreed development plan:** introduce a dedicated `ContextGraphNode3D extends MeshView` using a shared unit-sphere `TriangleMesh`; benchmark it before changing `AnimatedSphere` or `Projections3DPane`. Preserve per-node size, position, color, opacity, hover and selection semantics. Context Graph hover enlargement should not exceed 10%. Do not assume shared-sphere optimization yields a large gain: the original 1,000 spheres already loaded quickly.

## 4. Performance history and verified baseline

Reference dataset: **1,000 nodes and 6,024 edges** (~766,701-character graph JSON). Times below are measured on the user's test system; do not generalize to other machines or graph sizes.

| Experiment | Observed outcome |
|---|---|
| 1,000 `AnimatedSphere` only | ~1 s |
| ~6,000 JavaFX `Cylinder` only | ~10 s, UI freezes |
| Single batch of cylinders | ~12.1 s total, ~11.069 s FX pulse gap; geometry preparation ~75 ms, attachment ~61 ms |
| 50 cylinders/pulse | ~13.223 s, UI responsive but camera sluggish, max pulse gap ~145 ms |
| Initial 12 ms adaptive estimator | ~10% edges in 10 s; rejected as infeasible |
| AIMD adaptive with built-in cylinders | ~21.733 s, usually minimum 10 edges/pulse; rejected |
| Shared unit-cylinder mesh, 50/pulse | **2.329 s** progressive completion; ~2.504 s to all callbacks |
| Shared unit-cylinder mesh, **100/pulse** | **1.162 s** progressive completion; **1.376 s** to all callbacks; 61 batches |

The last 100/pulse run reported background geometry preparation 83.992 ms, edge preparation 66.843 ms, maximum batch attachment 1.788 ms, progressive-loader maximum pulse gap 32.648 ms, and separate pane-monitor maximum gap 92.191 ms. The two pulse monitors cover different windows; do not conflate them. This is the **last measured performance baseline**, not a guarantee for future changes.

**Conclusion from controlled tests:** the decisive optimization was shared `TriangleMesh` geometry. Batch scheduling alone did not remove the cylinder initialization cost. Preserve the shared-mesh edge architecture and the fixed 100-edge progressive loader unless new measurements justify changes.

## 5. Camera and Fit Graph — verified behavior

Camera uses a `CameraTransformer` parent for orbital pivot and yaw/pitch/roll, with `PerspectiveCamera` local translations. Negative camera-local Z represents orbital distance. Zoom changes camera-local Z, not the world pivot; this must work from behind the graph as well as in front. Pattern was reconciled with `Projections3DPane`.

Controls: mouse drag rotates; mouse wheel zooms; Shift accelerates wheel zoom; on platforms that remap Shift-wheel to horizontal scrolling, use X delta when Y delta is zero; Ctrl offers fine zoom. W/S moves along local Z, A/D strafes, Space/X moves vertically; Shift accelerates movement. Preserve the existing orbital feel.

Fit Graph computes the **displayed** graph bounds, including layout positions and coordinate scale, and accounts for camera orientation, SubScene dimensions/aspect ratio, field of view and desired viewport occupancy. It recenters the orbital pivot, resets camera-local X/Y offsets as appropriate, and calculates viewing distance. The editable **Fit Graph Percentage** spinner defaults to **85%** (10–100%, step 1%); it applies to the next Fit Graph operation, not automatically on spinner changes. The user verified these updates.

## 6. Controls, styling and state

`ContextGraphControlsPane` is a floating `LitPathPane`, not a modal dialog or embedded control inside the SubScene. It exposes Graph Layout, Graph Style and Display tabs. The right-click context menu opens the controls and Content Navigator and provides Fit Graph.

- Display: show/hide nodes and edges; editable progressive edge batch spinner (default 100, range 1–1,000, increment 10); editable Coordinate Scale spinner (0.1–100.0, step 0.1, default 1.0); editable Fit Graph Percentage spinner (10–100%, step 1%, default 85%).
- Graph Style: global node color/radius/opacity and edge color/width/opacity. Edge Width spinner minimum 0.25, increment 0.25. `GraphStyleControlsView` emits `GRAPH_STYLE_PARAMS_CHANGED`; controller receives and applies changes.
- Graph Layout: layout kind and algorithm-specific parameters; expensive computation belongs on a worker, with FX-thread result application and generation protection.

**Important historical bug:** an earlier diagnostic `TEST_EDGE_MATERIAL` was applied to every edge during loading and style updates, preventing Edge Color and Edge Opacity controls from working. A correction was supplied, but per-edge coloration and opacity through all lifecycle paths are **not yet comprehensively verified**. Inspect the current controller for remaining diagnostic overrides before implementing new color features.

Opacity may use cached 1×1 self-illumination textures rather than ordinary diffuse alpha. Verify actual JavaFX rendering behavior rather than assuming both are equivalent. Changing a shared mutable `PhongMaterial` affects all views that reference it; use explicit per-object materials or well-defined shared palette ownership.

## 7. Layout and graph data semantics

The controller supports force-directed and network/hierarchy layout families, including FR, ForceAtlas, Kamada–Kawai, stress majorization, spectral, radial tree, layered hierarchy and hyperbolic hierarchy options. Validate exact availability and implementation in the current checkout. Some methods create topology/weight structures and compute coordinates asynchronously. Layout results are stored separately from the original node vectors in `layoutPositions`; the displayed coordinates apply `coordinateScale`.

`refreshEdges()` and `rebuildGraph()` historically contain distinct edge-construction paths with different radial divisions. Consolidation is a **proposed cleanup**, not a prerequisite for the next node optimization. `updateEdgeEndpoints()` historically performs repeated graph node-ID lookup; profile before replacing. Layout/coordinate changes may update thousands of edge transforms on the FX thread and should be benchmarked at larger sizes.

Graph-to-FeatureCollection adaptation should preserve stable node identity, source metadata and linked text/image content where supported. Do not store graph domain state in JavaFX nodes.

## 8. Interaction status and remaining recovery

Node/edge mouse transparency and geometry picking were disabled during performance recovery. `ContextGraphInteractionController` includes handlers for node/edge hover and click events, but do not infer that geometry dispatch is currently active. Remaining work includes re-enabling picking incrementally, node/edge hover, selection, Content Navigator updates, click inspection, and selection feedback, while benchmarking frame rate and initialization impact. Selection/hover state must preserve original per-object styling.

Do not use `Node.userData` for picking. Use explicit typed registries and event handlers. Avoid rebuilding all geometry merely to change selection or color.

## 9. Agreed next four-phase development program

**Phase 1 — ContextGraphNode3D:** Design and implement a dedicated shared-unit-sphere `MeshView` class; keep `AnimatedSphere` unchanged. Integrate with `ContextGraphController` without changing edge rendering, camera, controls or layout behavior. Maintain individual size and interaction state. Avoid shared-mesh mutation after creation.

**Phase 2 — Benchmark and regression:** Compare old and new node-only and full-graph loads; test 1,000 and 10,000+ nodes when available; measure preparation, FX attachment, pulse gaps, time to responsiveness, and camera FPS. Confirm `Projections3DPane` remains unaffected. Verify node sizes, coordinate scaling, layout updates, reloads, cancellation and Fit Graph.

**Phase 3 — Per-object coloration:** Support independent node colors by label/category and edge colors by label/type, including runtime recoloring, global-vs-local precedence, opacity, selection restore, progressive attachment and layout changes. Test that recoloring one object never unintentionally recolors another. Compare per-object material allocation against cached palette materials with profiling. Do not assume label metadata fields without inspecting models.

**Phase 4 — Projections3DPane evaluation:** Inventory its `AnimatedSphere` and `Sphere` API usage, label/gradient coloring, material mutation, hover, selection, drag/drop and texture features. A `MeshView` is not drop-in compatible with `Sphere` typing or `setRadius()`. Consider a dedicated `ProjectionsNode3D` and explicit migration only after independent functional and performance verification and user approval. Do not globally replace `AnimatedSphere` as a shortcut.

## 10. Additional backlog (not yet verified complete)

- Restore picking/hover/click/Content Navigator interaction and meaningful edge inspection.
- Verify style persistence through progressive loading, graph reloads, layout changes and resets.
- Test larger graph workloads (e.g., 5k/20k, 10k/50k nodes/edges) and avoid arbitrary hard limits.
- Review `refreshEdges()` reconstruction, material cache growth and coordinate update costs.
- Validate hyperbolic 3D hierarchy behavior and navigation, layout cancellation, and optional transitions.
- Explore graph community detection, centrality, semantic relationships and multiscale document hierarchy browsing as separate product initiatives.
- For any MDS-based graph layout, reuse existing Trinity SuperMDS/metrics infrastructure rather than duplicating distance algorithms.

## 11. Contribution rules and threading

- **Use the latest authoritative source checkout.** Prior generated variants may differ, and manually verified fixes take precedence over old patches.
- No wildcard imports; avoid inner classes for reusable models/controllers; avoid controller `Consumer` patterns; prefer explicit types.
- No `Node.userData` for model relationships. Maintain typed registries/maps.
- Keep computational work and detached mesh preparation off the FX thread; scene attachment, controls and scene-graph mutations on the FX thread. A `Task` need not originate on the FX thread. Preserve progress animation and generation/cancellation checks.
- Use the existing `GraphEvent`, `ApplicationEvent` and `CommandTerminalEvent` conventions. Use `LitPathPane` for substantial floating overlays, not ad hoc modal dialogs. Avoid inline CSS.
- Do not introduce new dependencies or architectural rewrites without evidence and approval. Preserve working shared-cylinder mesh, progressive loader and camera behavior.
- Instrument using `System.nanoTime()` and existing `Utils.printTotalTime()` conventions; distinguish background preparation, FX attachment, pulse gaps and user-visible responsiveness. Pulse callback intervals are not direct GPU presentation timestamps.
- Report what was actually built/tested; never equate source inspection with an integrated runtime test.

## 12. Agent checklist before changes

1. Read root `AGENTS.md`, this document and the latest source files.
2. Identify whether the change affects graph model, geometry, rendering, layout, camera, controls, or interactions.
3. Confirm current APIs and data ownership, especially `AnimatedSphere` compatibility and per-object material behavior.
4. Specify the smallest change that preserves the verified 100-edge shared-mesh baseline.
5. Plan FX-thread boundaries, cancellation/generation checks and timing measurements.
6. Implement only the approved phase; do not silently modify `Projections3DPane` during Context Graph node work.
7. Test rendering, styles, scaling, layout, camera, progress indicator, and graph reloads.
8. Update this document when accepted architecture or measured baseline changes; put speculative initiatives in a roadmap rather than claiming they are implemented.

**Source of truth:** current code and user runtime verification override older handoffs and historical generated files.
