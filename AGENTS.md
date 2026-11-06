# Trinity XAI — Agent and Developer Guide

**Purpose:** Entry point for human developers and AI coding agents working in the Trinity repository. Read this file first, then the relevant subsystem documents. This is a living repository-root guide, not a replacement for current source code.

## 1. Project orientation

Trinity is an explainable AI and analytical visualization desktop application centered on high-dimensional `FeatureVector`/`FeatureCollection` data, interactive JavaFX 3D SubScenes, and supporting 2D floating overlays. It includes Hyperspace, Hypersurface, Projections/Manifolds, Context Graph 3D, Hyperdrive ingestion/embeddings/topic discovery, and related analysis tools.

The Hyperdrive developer guide identifies Java 25/JavaFX as the current target. The public README describes broader Java compatibility and Maven/Gradle build entry points. **Check the active checkout's build configuration and toolchain before choosing language APIs or build commands.** Do not assume older source snapshots reflect the current runtime.

Start with `README.md` for product orientation and existing build/runtime guidance. Consult the actual Maven/Gradle build files and repository scripts for setup and launch instructions; do not assume a separate developer quick-start document exists.

## 2. Repository-root documentation index

| Document | Read when | Contents |
|---|---|---|
| [`README.md`](README.md) | Entering the project | Product overview, 3D projections, FeatureVector/FeatureCollection, build/runtime guidance |
| [`HYPERDRIVE_DEVELOPER_CONTEXT.md`](HYPERDRIVE_DEVELOPER_CONTEXT.md) | Working on Hyperdrive | Accepted ingestion, embeddings, REST, JSON mapping, topic discovery, landmarks, events, overlays and testing contracts |
| [`HYPERDRIVE_ROADMAP.md`](HYPERDRIVE_ROADMAP.md) | Planning Hyperdrive extensions | Proposed confidence/abstention, topic quality, persistence, hierarchy, feedback, semantic search and other priorities |
| [`CONTEXT_GRAPH_3D_DEVELOPER_CONTEXT.md`](CONTEXT_GRAPH_3D_DEVELOPER_CONTEXT.md) | Working on Context Graph 3D | Graph architecture, shared-mesh rendering, measured performance, camera/controls, interaction status and phased node optimization |

**Naming:** Place the uploaded Hyperdrive documents at the canonical root filenames shown above (remove upload suffixes such as `(1)`). These links are intended to be relative and work in a GitHub checkout. Add future subsystem guides to this table as they are accepted. A dedicated Context Graph roadmap may be added later; do not present one as existing until created.

## 3. Authority and change control

1. **Current checkout is authoritative.** Ask for the latest source snapshot if working outside the repository. Generated patches, prior chat summaries and historical ZIP archives may predate user-applied fixes.
2. **Preserve verified behavior.** Do not silently regress working camera controls, rendering performance, request throttling, data integrity, or UI interactions to simplify a new feature.
3. **Separate implemented, verified, proposed and experimental.** A roadmap item is not a shipped feature. A generated source file is not a tested change.
4. **Work in approved phases.** Discuss architecture and compatibility before large changes. Do not expand scope across subsystems without agreement.
5. **Prefer minimal deltas.** Avoid broad rewrites, framework changes, new dependencies or unrelated cleanup in a focused fix.

## 4. Coding conventions

- Use explicit Java imports; **no wildcard imports**.
- Prefer reusable top-level classes over inner classes for models, mapping rows, payloads and controllers.
- Do not use `Node.userData` for domain objects, picking, or scene-graph mappings. Use explicit typed maps/registries and JavaFX event handlers.
- Avoid `Consumer`-driven controller APIs when explicit methods and event contracts are clearer.
- Do not introduce arbitrary dataset-size thresholds or caps without a measured requirement.
- Prefer established Trinity utilities, metrics, events, data models and UI conventions over parallel implementations.
- Avoid inline CSS; use existing stylesheets and style classes.
- Keep source identity and metadata intact when converting, projecting, classifying or displaying data.

## 5. JavaFX architecture and concurrency

- Heavy parsing, clustering, layout computation, REST calls and geometry preparation belong off the JavaFX Application Thread, usually in a `Task` or the established worker pattern.
- Constructing detached `TriangleMesh` geometry off the FX thread is supported by the project's established design; do not incorrectly insist all mesh construction must occur on the FX thread.
- Attach/mutate active scene-graph nodes and controls on the FX thread. Use `Platform.runLater()` or the existing pulse-based attachment approach as appropriate.
- Preserve cancellation and generation checks so stale background results cannot overwrite a newer view.
- Progress indicators and camera interaction must remain responsive during large operations. Measure pulse gaps and time to responsiveness, not only CPU construction duration.
- Keep domain calculations out of UI controls; communicate between independently managed panes through the relevant event system.
- SubScene controls belong in existing right-click menus and/or `LitPathPane` floating overlays, not arbitrary embedded widgets. Use blocking `Alert` only when appropriate for concise preflight intervention.

## 6. Domain-specific rules and critical baselines

**Hyperdrive:** Read its developer context before changing ingestion, JSON schema mapping, embedding request management, topic discovery, REST response models or landmark classification. Preserve managed request batching, stable input/response identity, typed Jackson POJOs, original N-dimensional embedding computations, and event-decoupled floating overlays. Use `CommandTerminalEvent` for status/warnings and SLF4J for detailed diagnostics.

**Context Graph 3D:** Read its developer context before changing geometry, layouts, camera, styling or interactions. The verified edge optimization uses shared unit-cylinder `TriangleMesh` geometry with independent `MeshView` transforms and fixed progressive attachment, default **100 edges per pulse**. The measured 1,000-node/6,024-edge graph completed progressive edge loading in **1.162 seconds**. Preserve the corrected local scale-before-rotate ordering and the verified orbital camera/Fit Graph controls. Shared mesh geometry must not accidentally force shared per-object color state. The next approved program is a dedicated `ContextGraphNode3D`, benchmark, per-object coloration, then possible Projections-specific optimization; do **not** globally replace `AnimatedSphere`.

**Projections and other 3D views:** Audit existing `Sphere`-specific API, label-color updates, selection, texture and event behavior before introducing `MeshView` replacements. Do not assume scaling relaxations for solid-color Context Graph edges apply to textured objects.

## 7. Verification and delivery

Before implementation, identify affected classes, event contracts, threading boundaries and regression risks. Add focused tests for pure logic; for JavaFX changes, verify runtime behavior on representative datasets and check camera responsiveness, picking, colors/opacity, scale/layout updates and cancellation. Run the project build where possible and report build and runtime verification separately.

For performance work, retain a reproducible dataset, measure preparation time, FX attachment time, pulse gaps and end-to-end completion. Compare against the last verified baseline; do not infer native GPU allocation details solely from FX callback intervals.

Deliver complete files or source-only deltas as requested. Name all replacements/deletions, avoid unrequested unrelated files, and state clearly when code has not been compiled or runtime-tested. When a change is accepted, update the relevant context guide and roadmap so the next developer inherits the current state.

## 8. Quick-start workflow for incoming agents

1. Read `README.md`, this `AGENTS.md`, and the relevant subsystem context document.
2. Inspect the actual current source and build configuration; identify stale documentation or conflicts.
3. Summarize the affected architecture, accepted behavior and proposed minimal change.
4. Confirm requirements and compatibility boundaries before implementation.
5. Implement in the correct worker/FX-thread layers, retaining generation and cancellation protections.
6. Build/test to the extent possible; clearly distinguish static checks from user-verified runtime behavior.
7. Update the appropriate subsystem document and roadmap when decisions or verified functionality change.

**Principle:** Optimize and extend Trinity without erasing working behavior, data provenance, or the measured gains already achieved.
