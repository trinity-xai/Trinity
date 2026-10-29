# Hyperdrive Developer Context and Implementation Guide

**Purpose:** Provide human and AI-agent developers with a shared understanding of Hyperdrive's workflow, its accepted design decisions, and the constraints to preserve when extending it.

**Project:** Trinity XAI — Java 25 / JavaFX application  
**Subsystem:** Hyperdrive data ingestion, embeddings, topic discovery, landmark classification, and chat/vision service integration  
**Document status:** Living development handoff. Update it when a workflow or architectural decision changes.

---

## 1. Mission and scope

Hyperdrive turns user-provided content into high-dimensional `FeatureVector` data so analysts can explore, categorize, and classify content using Trinity's existing visualization and analysis infrastructure.

The intended workflow is not only "send content to an embedding endpoint." It includes:

1. Load and parse text or structured data.
2. Define which source fields represent semantic content and which should remain metadata.
3. Request embeddings while respecting model-service capacity.
4. Discover possible topics from the embeddings when the analyst does not already know suitable categories.
5. Review, name, summarize, and downselect the discovered topics.
6. Generate topic landmarks from the selected clusters.
7. Use the existing distance-based landmark classifier to assign labels to source chunks.
8. Inspect results and refine the analyst's categories or workflow.

Hyperdrive should preserve source identity and metadata wherever possible. It must not silently substitute raw or structurally noisy input when a configured extraction mapping fails.

## 2. End-to-end workflow

### 2.1 Text ingestion and structured JSON extraction

Text loading starts in Hyperdrive's Text tab. Parsing and chunking should be performed by the non-UI processing layer (`TextChunkParser` and related model classes), not embedded in JavaFX view code.

For JSON, Services exposes three processing modes:

- **Disabled:** treat JSON as ordinary text.
- **Legacy:** use the existing automatic/special JSON extraction path.
- **Schema-Mapped:** use a selected `JsonExtractionProfile` to select fields for embedding input, labels, and metadata.

Schema mapping is configured in the dedicated floating JSON Schema Mapping pane. The pane should support loading a representative JSON sample, discovering nested field paths (including array paths), configuring field roles, previewing the exact extracted embedding text, and saving/loading reusable profiles.

Schema mapping is applied during import. Changing a profile does not retroactively alter existing `FeatureVector` objects or their embeddings. To change the representation, reimport and regenerate the embeddings.

A mapping profile separates three concepts:

- **Embedding text:** fields that contribute semantic content to the model input.
- **Label:** the value used as an initial human-readable record label, if configured.
- **Metadata:** source properties retained for traceability and later inspection but not embedded unless explicitly selected.

For the database-attribute example, an appropriate initial profile embeds `attribute_name`, `properties.definition`, and `properties.note`; metadata may include datatype, entity name, validation rule, source file, and source field. This is an example, not schema-specific logic that should be hardcoded in the parser.

If a Schema-Mapped profile has no embedding fields, fail visibly. If extraction yields no semantic text for a record, skip/report it rather than silently falling back to serialized JSON.

### 2.2 Embedding requests and service capacity

Services configuration controls request batching and dispatch parameters, including:

- chunks per embedding request (`batchSize` / Request Batch Size),
- maximum concurrent batches (`maxInFlightBatches` / Max In Flight Batches),
- request pacing delay,
- request timeout.

Text embedding requests support multiple input strings per request. The request manager must honor the configured batch size and concurrency, pace dispatches, and release in-flight capacity exactly once for every completion/failure/timeout path.

Text and image embedding workflows use distinct request managers/accounting. Preserve the accepted managed request infrastructure. Avoid reviving the older unbounded one-request-per-chunk text path or creating a second competing throttling mechanism.

Response association must use stable item identities and the embedding response indices where supplied; never assume a filtered input list remains positionally aligned with the original IDs.

The standalone Services model-test buttons are not the same thing as production bulk ingestion. Test paths should reuse the managed REST capabilities where appropriate, but should not register their standalone operation as a fake production batch or interfere with a running job.

### 2.3 Topic discovery

Topic discovery runs on already embedded text chunks. It uses the **original N-dimensional embedding vectors**, not UMAP coordinates or other 2D/3D projections. Projection coordinates can distort distances and must not become the input to semantic clustering.

The implementation uses Trinity's existing clustering library/implementations. Current method choices include K-Means, K-Medoids, DBSCAN, HDBSCAN (enum spelling `HDDBSCAN`), Affinity Propagation, and Gaussian Mixture (`EX_MAX`). Parameter controls are method-specific.

`TopicDiscoveryEngine` is intended to be Java-only and independent of JavaFX so it can run in a worker thread and be tested separately. Its main data model includes:

- `Sample`: source chunk ID, source text, embedding vector.
- `Options`: cluster method and algorithm parameters.
- `Result`: discovered topics, noise count, and sample count.
- `Topic`: members, centroid, representative text, keywords, cohesion, distinctiveness, name, summary, and inclusion state.

The implementation normalizes a working copy of input vectors for clustering. Topic centroid prototypes are computed from the original vectors so that the generated landmark remains on the same embedding scale as the source data and remains compatible with the user-selected landmark distance metric.

Topic representation is distinct from clustering:

- Clustering determines which chunks are members of each topic.
- Contrastive / class-based TF-IDF-style keyword ranking finds terms that distinguish one cluster from the others.
- Optional chat-model summarization creates a readable title and one-sentence description from keywords and representative passages.

The default topic names are placeholders plus keyword suggestions until a human edits the title or the chat model returns a summary. LLM summarization must not be treated as proof that all cluster members are coherent; representative excerpts need review.

K-Means normally returns the requested number of clusters when its input/parameters are valid. Density-based methods may produce fewer topics or noise points. A valid empty/low-topic result must be reported rather than treated automatically as an exception.

### 2.4 Topic review and landmark generation

The Topic Discovery Review view is a **floating `LitPathPane` overlay**, not a modal JavaFX `Dialog`. The JSON Schema Mapping view is also a floating overlay. Overlay lifecycle is managed by `AppAsyncManager`, and panes communicate with Hyperdrive through `HyperdriveEvent`; new panes should not keep a direct `HyperdrivePane` reference.

The review workflow supports inspecting a topic, editing its title and description, optionally asking the configured chat model to summarize it, merging selected topics, and toggling whether a topic is included in landmark generation.

Merging changes membership and recomputes the merged cluster's centroid and representation. Renaming or editing a summary changes descriptive fields, not the vector representation.

`Generate Selected Landmarks` uses topics whose **Include in generated landmarks** option is checked; it is not limited to the currently highlighted row. A generated landmark carries the discovered topic's centroid vector. It should not be refreshed by embedding its display label, because that would overwrite the cluster-derived prototype with an embedding of the name.

Generating landmarks does not automatically classify source chunks. Classification remains a separate, explicit analyst action.

### 2.5 Landmark similarity classification

Hyperdrive already had vector-distance-based nearest-landmark classification before Topic Discovery was added. The new Java-only similarity engine is a refactoring/hardening of that behavior, not a new similarity algorithm.

For each selected source chunk, compare its valid embedding to each valid landmark embedding using Trinity's existing `Metric` framework and the selected Landmark Assignment Metric (cosine distance is the typical/default choice). The nearest valid landmark receives the label.

The engine should validate at least:

- nonempty usable landmark set,
- actual completed embeddings (not placeholder vectors),
- matching vector dimensionality,
- finite values and finite distance results,
- cosine/angular inputs that are not zero vectors.

Similarity calculation should occur away from the FX thread. Apply UI changes/results on the JavaFX Application Thread. Validate before mutation and preserve pre-existing labels for chunks not successfully classified or when the operation is cancelled/aborted.

The current classifier always chooses the nearest valid landmark. A calibrated abstention threshold and a first-class unclassified outcome are future roadmap work, not behavior to assume exists today.

### 2.6 Chat, vision, and embedding REST integration

Trinity uses Jackson `ObjectMapper` POJO binding for request/response JSON. Do not replace this with a custom manual response parser or generic `JsonNode` response layer without a strong, reviewed reason.

The Chat/Vision response model was extended based on a real Qwen response that included:

- `generation_parameters.stop` as nested arrays (observed shape: `[[""]]`),
- `message.reasoning_content`,
- token `usage` values (`prompt_tokens`, `completion_tokens`, `total_tokens`).

The response model uses typed classes (including a typed `Stop` wrapper and `ChatUsage`) rather than blindly coercing the response into a string. The outgoing chat request's `stop` field is a separate request concern; do not change it merely because the response's `stop` representation differs.

Topic summarization needs a sufficient completion budget for reasoning-capable models. Its configured output token allowance was increased to 1024 after the smaller budget produced an empty final content result. `reasoning_content` must not be substituted for the user-facing summary. Diagnostics should inspect finish reason, token usage, reasoning length, and final content presence without logging prompts, document passages, or reasoning text unnecessarily.

Image embedding requests require an image-capable embedding model. A text-only model can return a gateway HTTP 502 that wraps an upstream HTTP 400 such as `is not a multimodal model`. This is a user/model selection issue: report a `CommandTerminalEvent` warning naming the selected model, rather than asserting the image encoder is broken. Other failures should remain errors with diagnostic details.

### 2.7 User notifications

Use Trinity's existing `CommandTerminalEvent` patterns for the bottom-left singleton status line:

- success notifications for completed operations,
- warnings for user-correctable configuration/capability problems and partial outcomes,
- errors for actual failed operations.

Use JavaFX Alerts only for blocking preflight validation that needs user intervention before an operation can start. Do not present one modal alert per failed record. Aggregate partial exclusions and errors into one summary and log detailed diagnostics using SLF4J.

Apply `dialogstyles.css` to actual JavaFX dialogs/Alerts where the codebase convention requires it. The floating JSON Schema Mapping and Topic Review views are `LitPathPane` overlays, not dialogs; do not convert them back to dialogs. Use existing overlay styles and lifecycle patterns.

---

## 3. Key implementation areas

Paths below are relative to `src/main/java/edu/jhuapl/trinity/` unless stated otherwise. Verify exact class locations in the current checkout before editing; source snapshots and deltas may lag local manual changes.

| Area | Important implementation | Responsibility |
|---|---|---|
| Main Hyperdrive UI | `javafx/components/panes/HyperdrivePane.java` | Tabs, Services controls, task orchestration, result application, notifications |
| Text parsing | `javafx/components/hyperdrive/TextChunkParser.java` | Format parsing, chunk creation, JSON profile application, CSV handling |
| JSON field model | `javafx/components/hyperdrive/JsonExtractionProfile.java` and related mapping classes | Selected fields, profile persistence, extraction of text/label/metadata |
| JSON overlay | `javafx/components/hyperdrive/JsonSchemaMappingPane.java` in the latest accepted overlay design | Schema inspection, mapping editor, preview, apply/save/load profile |
| Topic discovery | `javafx/components/hyperdrive/TopicDiscoveryEngine.java` | Clustering on embeddings, topic membership, centroids, representative chunks, topic quality/keywords |
| Topic discovery UI | `javafx/components/hyperdrive/TopicDiscoveryPane.java` | Compact discovery controls and candidate list |
| Topic review overlay | `javafx/components/hyperdrive/TopicDiscoveryReviewPane.java` in the latest accepted overlay design | Inspect/edit/merge/summarize/include topics and request landmark generation |
| Similarity engine | `javafx/components/hyperdrive/TextLandmarkSimilarityEngine.java` | Pure-Java input validation and nearest-landmark calculations |
| Similarity task | `javafx/components/hyperdrive/RequestTextLandmarkSimilarityTask.java` | Worker/task coordination and result application lifecycle |
| Request management | `javafx/components/hyperdrive/BatchRequestManager.java`, launchers and REST access layer | Batching, concurrency, pacing, timeout and completion/failure accounting |
| Event decoupling | `javafx/events/HyperdriveEvent.java` | Typed event types/payload transport between panes and coordinator |
| Overlay ownership | `javafx/AppAsyncManager.java` | Create, attach, close and manage floating panes |
| Status line | `javafx/events/CommandTerminalEvent.java` | Success/warning/error status notifications |
| Chat models | `data/messages/llm/*`, `messages/ChatCompletionCallback.java` | Typed Jackson POJOs and asynchronous response processing |

The class names for the floating views reflect the refactor after the `src(4).zip` snapshot. That archive predates the final overlay conversion and manual import fixes, so **do not treat `src(4).zip` as a complete representation of the currently verified tree**. The latest user-provided source tree and manually accepted fixes are authoritative.

---

## 4. Design and contribution rules

These rules are requirements for future Hyperdrive work.

### 4.1 Preserve separation of concerns

- Keep parsers and pure computation separate from JavaFX controls.
- Perform blocking parsing, clustering, LLM requests, and similarity calculations on background tasks as appropriate.
- Keep JavaFX scene graph updates on the JavaFX Application Thread.
- Treat views as presentation/controller surfaces, not as the source of domain algorithms.
- Avoid mixing source parsing, UI layout, request scheduling, and result classification into a single new class.

### 4.2 Decouple panes with events

- Use `HyperdriveEvent` (extend it when appropriate) for communication between Hyperdrive's independently managed panes.
- Let `AppAsyncManager` manage floating pane creation and lifecycle.
- Do not pass a direct `HyperdrivePane` reference into JSON mapping or topic review overlays.
- Add explicit event types and document payload expectations. Avoid opaque event payloads when a small typed payload record/class would make the contract clearer.
- Unregister listeners and close stale overlays on lifecycle transitions, such as rerunning discovery.

### 4.3 Java code standards

- **Never use wildcard imports.** Import each class explicitly.
- **Avoid inner classes wherever possible.** Put reusable UI rows, mapping rows, event payloads, and domain models in separate top-level files unless there is a compelling, documented reason.
- Prefer the project's existing Java style, explicit types, meaningful names, and minimal scope changes.
- Don't add dependencies or new frameworks when Trinity already provides an applicable implementation.
- Don't use ad hoc JSON parsing when a correct Jackson POJO mapping is possible.
- Keep request DTOs and response DTOs separate where wire schemas differ.

### 4.4 JavaFX UI conventions

- Use `LitPathPane` floating overlays for substantial secondary workspaces, including JSON Schema Mapping and Topic Review.
- Use JavaFX `Dialog`/`Alert` only for concise blocking interaction or validation.
- Apply the established `dialogstyles.css` to dialogs/Alerts as required; overlays should follow the existing `LitPathPane` and stylesheet conventions.
- Keep the Similarity tab's overview controls compact; put detailed mapping and topic review in overlays.
- Avoid introducing an additional main tab for a feature that belongs naturally to an existing workflow.

### 4.5 Correctness and data integrity

- Do not silently fall back to raw JSON when Schema-Mapped extraction is misconfigured or yields no useful text.
- Do not embed schema keys or metadata unless selected intentionally as semantic input.
- Preserve source IDs, labels, source paths, chunk/record indices, and selected metadata where the data model supports them.
- Validate response indices and correlate results to their original request items; do not assume a filtered or reordered list preserves positional identity.
- Do not use unembedded placeholder vectors in clustering or similarity.
- Do not overwrite cluster-derived centroid landmarks by embedding their display names.
- Keep topic discovery and landmark generation non-destructive: discovery must not silently relabel source chunks.
- Preserve existing labels when a classification result is invalid, skipped, cancelled, or fails.
- Distinguish a model-capability/user selection issue from an infrastructure/server failure in status messages.

### 4.6 Testing and verification

- Add pure Java tests for parser logic, keyword ranking, clustering-result handling, vector validation, and similarity math where practical.
- Test exact-size chunk boundaries, empty content, malformed structured data, nested JSON field paths, mixed line endings, and missing mapping fields.
- For asynchronous REST workflows, test success, HTTP error, connection failure, timeout, cancellation, malformed response, and out-of-order completion.
- When a feature spans multiple panes, test the event contract and overlay lifecycle as well as the pure computation.
- Verify the target runtime/build where possible. Never describe static inspection or an isolated smoke test as a full Trinity build/runtime test.
- Deliver changes as source-only deltas against the latest user-supplied authoritative tree when requested. Include a manifest and clearly list deletions/replacements.

---

## 5. Accepted behavior and current verification

The following have been reported as working by the user and should be preserved unless a new defect is reported:

- Text embedding batching and Services-tab request settings.
- Text ingestion corrections covering PDFs, CSV quoting, JSON object/array handling, chunk boundaries, embedding-text consistency, provenance, and FX-thread UI creation.
- Text landmark similarity validation and stale/incomplete embedding handling.
- Topic discovery using original embedding vectors and the existing clustering implementations.
- Schema-mapped JSON field extraction, nested field selection, preview, profiles, labels, and metadata.
- LLM topic summarization after increasing the summary completion budget.
- Typed chat response model additions for nested `generation_parameters.stop`, `reasoning_content`, and token `usage`.
- Image embedding test warning when the selected embedding model is not multimodal.
- Floating-overlay approach for JSON Schema Mapping and Topic Review, with event-based decoupling.
- Topic discovery and selected centroid landmark generation producing useful results for the user's tested dataset.

Some UI refactor changes were manually corrected by the user after earlier deltas (notably explicit static imports and extraction of `MappingRow` into its own class). Preserve those corrections. The last complete source archive may lag subsequent accepted patches; request/use the most recent source snapshot before any future implementation.

---

## 6. Current known limitations (not assumed to be fixed)

- Nearest-landmark classification always picks the nearest valid landmark; a calibrated reject/abstain threshold remains proposed work.
- Topic quality measures are useful diagnostics, not proof of semantic coherence or calibrated confidence.
- Chat summaries are based on a small set of representative excerpts, not necessarily every cluster member.
- Topic discovery results/experiments are not yet described as having durable, first-class persistence. Treat a persistent Topic Discovery Result model as a roadmap proposal until verified in the current tree.
- Clustering method quality varies by data and parameters. Density-based methods may label points as noise; K-Means will still partition mixed semantic content if asked to produce a fixed number of groups.
- Do not infer current UI/source behavior from a stale delta or source archive. Inspect the latest source tree first.

---

## 7. Quick checklist before making a Hyperdrive change

1. Identify the specific workflow stage: ingestion, embedding, discovery, review, landmark generation, classification, or reporting.
2. Inspect the latest source tree and existing event/request/overlay conventions.
3. Determine whether the change affects source data, embeddings, labels, or only display metadata.
4. Preserve stable identity between source records, request items, embedding responses, and resulting FeatureVectors.
5. Keep panes decoupled with events; keep algorithms out of UI classes.
6. Use explicit imports and top-level classes; no wildcard imports.
7. Decide how errors are surfaced: blocking Alert for invalid preflight, status-line summary for handled/partial outcomes, logs for diagnostic detail.
8. Add focused tests, then run the full project build/runtime checks when the build tree is available.
9. Report exactly what was changed and what was actually tested.
