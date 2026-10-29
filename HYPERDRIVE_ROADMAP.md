# Hyperdrive Roadmap

**Purpose:** Capture candidate improvements to Hyperdrive after the successful implementation of structured text extraction, topic discovery, topic summarization, centroid-based landmarks, and existing landmark similarity classification.

**Status:** Proposed roadmap, not a commitment that these features already exist. Priorities reflect expected analytical value and dependencies.

---

## 1. Current baseline

Hyperdrive now supports the following end-to-end workflow, verified by runtime testing on the current development dataset:

1. Import text and structured JSON.
2. Configure JSON field extraction through a reusable schema-mapping profile.
3. Generate embeddings using managed text/image request workflows.
4. Discover candidate topics by clustering original N-dimensional text embeddings.
5. Inspect topic statistics, keywords, and representative passages.
6. Use an LLM to generate a topic name and short description.
7. Merge, edit, include, or exclude candidate topics.
8. Generate landmark prototypes from selected cluster centroids.
9. Use the existing distance-based classifier to assign text chunks to the nearest valid landmark.

The roadmap below extends this working workflow. Future changes must preserve the accepted data-ingestion, batching, model-response, floating-overlay, event-decoupling, and landmark-similarity behavior.

## 2. Prioritization overview

| Priority | Initiative | Main value | Dependencies |
|---|---|---|---|
| P1 | Confidence-aware classification and abstention | Avoid false certainty when a chunk matches no landmark well | Existing similarity engine |
| P1 | Topic quality diagnostics | Distinguish useful semantic clusters from mathematically valid but poor clusters | Existing topic metrics and representatives |
| P2 | Topic experiment comparison | Compare clustering algorithms and parameters without losing prior results | A persistent discovery-result model |
| P2 | Hierarchical topic discovery | Move between broad categories and useful subtopics | Topic hierarchy model and UI |
| P2 | Analyst feedback and prototype refinement | Improve landmarks with human review | Stable record/topic identities and review workflow |
| P3 | Semantic search | Retrieve relevant chunks without requiring a category label | Existing embeddings and model configuration |
| P3 | Duplicate and near-duplicate detection | Detect repetition that distorts topic sizes and review | Text normalization and vector similarity |
| P3 | Richer topic representation | Improve keywords and evidence-grounded summaries | Current c-TF-IDF-style keyword ranking and chat workflow |
| Foundation | Persistent Topic Discovery Result model | Make experiments reproducible and support comparison/refinement | Needed by several P2/P3 items |

Recommended sequencing: implement P1 classification reliability first, then topic quality diagnostics. Introduce persistence before building substantial experiment comparison or hierarchical workflows.

---

## 3. P1 — Confidence-aware landmark classification

### Problem

The existing classifier selects the nearest valid landmark. Even when every landmark is a poor semantic match, the chunk still receives the label of the least-distant option. This can create misleading labels that appear definitive.

### Proposed capabilities

- Compute and retain the closest and second-closest landmark distances.
- Report the best-match distance and the margin between the best and runner-up.
- Support a configurable reject/abstain policy that leaves a chunk unclassified when evidence is insufficient.
- Introduce an explicit `Unclassified` result/status rather than inventing a category.
- Identify ambiguous assignments where two or more landmarks are nearly equally close.
- Provide a review list of low-confidence and ambiguous chunks.
- Keep the existing user-selected distance metric and avoid interpreting raw distance as a probability.

### Design notes

- Distance thresholds are model- and dataset-dependent. Do not choose a universal threshold without validation.
- A margin score measures separation between candidate landmarks; it is not a calibrated confidence probability.
- Validate thresholds against human-reviewed examples and show the threshold/metric used for each run.
- Preserve original labels for skipped, invalid, cancelled, or failed cases.
- Do not change the existing nearest-landmark result silently. Provide an explicit configuration or mode for abstention.

### Acceptance criteria

- A record that is close to no landmark can remain unclassified when configured to do so.
- A record almost equally close to two landmarks is flagged as ambiguous when configured.
- Existing nearest-landmark behavior remains available.
- Reports show counts for classified, unclassified, ambiguous, skipped, and failed records.
- Tests cover missing vectors, incompatible dimensions, invalid numeric values, ties, cancellations, and threshold boundaries.

---

## 4. P1 — Topic quality diagnostics

### Problem

A clustering algorithm can produce clusters even when the source data does not contain that many coherent semantic topics. Topic names/summaries alone cannot prove quality.

### Proposed capabilities

For each topic, provide:

- member count and proportion of the selected dataset,
- within-topic cohesion,
- distance/similarity to nearest competing topics,
- representative passages nearest to the centroid,
- examples near the cluster boundary (not only central examples),
- keyword overlap with other topics,
- warning indicators for very small clusters, low cohesion, excessive overlap, or mixed representatives.

For a whole clustering run, provide:

- number of topics and noise/unassigned points,
- percentage of source chunks represented,
- topic-size distribution,
- duplicate or near-duplicate topic descriptions,
- optional quality statistics supported by the selected algorithm and metric.

### Design notes

- Treat every score as a diagnostic, not a guarantee of semantic truth.
- Explain the meaning and scale of each statistic in the UI.
- Do not compare scores from different metrics or algorithms as if they had identical scales.
- Keep representative text visible so an analyst can judge quality directly.
- LLM-generated descriptions should be visibly distinguished from algorithmic metrics and analyst-edited content.

### Acceptance criteria

- An analyst can quickly identify dominant, small, diffuse, and overlapping topics.
- A topic can be flagged for review without automatically being discarded.
- The UI exposes enough evidence to diagnose whether poor labels originate in extraction, embeddings, clustering, or representation.

---

## 5. Foundation — Persistent Topic Discovery Result model

### Motivation

Topic discovery runs currently involve a related bundle of source identities, embeddings, cluster assignments, centroids, keywords, summaries, and analyst edits. Persistence would make comparing, revisiting, and refining runs safer and more reproducible.

### Proposed model

A durable discovery result should preserve:

- unique run ID and creation timestamp,
- selected source record/chunk IDs,
- source dataset identity or import-profile reference,
- embedding model identifier and dimensions,
- clustering method and full parameter configuration,
- cluster membership per source record,
- noise/unassigned records,
- centroid/prototype vectors,
- keyword results and topic metrics,
- topic title/summary and whether each is machine-generated or analyst-edited,
- inclusion/exclusion decisions,
- landmarks generated from the run and their provenance.

### Design notes

- Define serialization and versioning deliberately. Avoid storing JavaFX nodes in the result model.
- Keep domain results separate from views.
- Preserve the source identity needed to map a saved topic back to the current FeatureCollection.
- Handle stale/missing source IDs explicitly rather than silently remapping by list position.
- Consider storage size for embeddings and repeated content; avoid duplicating large source strings unnecessarily.

### Acceptance criteria

- A prior run can be reopened without rerunning clustering or LLM summarization.
- The selected method and parameters are visible and reproducible.
- A saved topic's members and prototype can be traced to source records.
- A subsequent experiment does not overwrite an earlier result.

---

## 6. P2 — Topic experiment comparison

### Problem

The number and shape of natural topics are not known in advance. Analysts need to compare parameter sets and algorithms rather than rely on one run.

### Proposed capabilities

- Run and retain multiple clustering configurations against the same embedded dataset.
- Compare K-Means settings (for example K=5 versus K=10) with density-based methods such as HDBSCAN.
- Compare topic count, noise rate, size distribution, cohesion, distinctiveness, keyword overlap, representative passages, and summaries.
- Allow the analyst to promote one run as the current accepted topic set.
- Preserve alternate runs so a change of method does not destroy previous manual topic edits.

### Dependencies

A persistent Topic Discovery Result model is recommended first.

### Acceptance criteria

- Two runs can be compared side by side using the same source dataset.
- The UI clearly flags when runs use different source selections, models, dimensions, or normalization assumptions.
- Promoting a run is explicit and does not immediately relabel source chunks.

---

## 7. P2 — Hierarchical topic discovery

### Motivation

A flat category list forces an analyst to choose between broad and narrow categories. Real datasets often have multilevel semantic structure.

### Proposed capabilities

- Discover or create parent/child relationships among topics.
- Support broader categories such as `Communications` with child topics such as `RF Interference`, `Link Degradation`, and `Protocol Failures`.
- Allow the analyst to select whether to generate landmarks at the parent level, child level, or a mixed level.
- Show topic size and representative evidence at each level.
- Support expanding and collapsing topic trees in the Review Topics overlay.

### Possible approaches

- Hierarchically cluster existing topic centroids.
- Recursively cluster members of a selected topic.
- Let the analyst manually group existing topics into a parent category.

Start with a controlled, explainable approach rather than assuming an automatically generated hierarchy is always semantically correct.

### Acceptance criteria

- A topic can have a parent without losing its original membership/provenance.
- Parent centroids/prototypes have a documented derivation.
- Landmark generation clearly identifies the hierarchy level chosen.
- Merging, renaming, or removing a parent does not silently corrupt child topics.

---

## 8. P2 — Analyst feedback and landmark refinement

### Motivation

Topic discovery and landmark classification should become an iterative workflow. Analysts will encounter incorrect labels, mixed topics, missing categories, and useful exceptions.

### Proposed capabilities

- Let analysts mark a classification as correct, incorrect, ambiguous, or unclassified.
- Allow moving a reviewed source chunk to a different category.
- Select verified examples as representative evidence for a topic.
- Recompute a topic prototype from an approved membership set.
- Add a new topic from reviewed examples when they reveal a missing category.
- Use the feedback to refine candidate landmarks or suggest a new discovery run.

### Design notes

- Separate the classifier's prediction from the analyst's accepted label.
- Retain provenance for manual corrections.
- Do not automatically alter an embedding vector when only the display label changes.
- Make prototype recomputation an explicit action and retain the prior version.
- Any future use of reviewed labels for supervised training should be a distinct, explicit feature.

### Acceptance criteria

- The UI can display both the predicted label and analyst-confirmed label.
- Manual corrections survive a rerun of classification unless the user explicitly asks to replace them.
- Prototype refinement documents which records contributed to the vector.
- A feedback action does not silently trigger new embedding requests.

---

## 9. P3 — Semantic search over embedded content

### Motivation

Landmark classification answers, "Which category best fits this chunk?" Semantic search answers, "Which chunks are most relevant to this question?" Both can use the same embedding collection while serving different analytical needs.

### Proposed capabilities

- Enter a natural-language query.
- Embed the query using a compatible text embedding model.
- Compare the query vector with existing text vectors using a chosen supported metric.
- Rank and display the closest chunks with source label, record identity, and metadata.
- Allow opening/inspecting the original source record.
- Optionally filter results by assigned topic/landmark.

### Design notes

- Verify that the query and corpus embeddings use the same model and dimensionality.
- Do not mix vectors from incompatible embedding models.
- Display the metric and score semantics; do not present raw distance as relevance probability.
- Preserve a path from a result to the source file/record when provenance is available.

### Acceptance criteria

- A query returns ranked results without requiring landmark labels.
- Results are traceable to source chunks and metadata.
- Invalid model/dimension combinations are rejected clearly.

---

## 10. P3 — Duplicate and near-duplicate detection

### Motivation

Repeated records and boilerplate can distort cluster sizes, keywords, centroids, and analyst perceptions of topic prevalence.

### Proposed capabilities

- Detect exact duplicates after a configurable text-normalization step.
- Detect near-duplicates using text similarity and/or embedding distance.
- Group duplicates for review rather than deleting them automatically.
- Show how duplicate removal or downweighting would change topic size and representation.
- Preserve links to all source records, even when one representative is chosen.

### Design notes

- Exact duplicates and semantic near-duplicates are different concepts; report them separately.
- Do not automatically delete source records or metadata.
- Avoid hiding repeated records if their repetition is analytically meaningful (for example, event frequency).
- Make normalization rules inspectable and consistent across a run.

### Acceptance criteria

- The analyst can inspect duplicate groups and choose how to handle them.
- Counts distinguish physical records, unique text values, and clustered unique representatives where applicable.
- Source provenance remains intact after any deduplication view.

---

## 11. P3 — Richer topic representation

### Current foundation

Topic Discovery V2 uses contrastive/class-based TF-IDF-style ranking so words that appear across every topic receive less weight than terms distinctive to a specific cluster. The current workflow can also request LLM-generated titles and one-sentence descriptions based on keywords and representative passages.

### Potential improvements

- Extract meaningful multi-word phrases, not just individual tokens.
- Improve stopword configuration for technical and domain-specific datasets.
- Offer a reviewable list of candidate phrases and their scores.
- Use semantic ranking to identify phrases that represent the cluster centroid.
- Provide LLM summaries with explicit evidence excerpts and warn when evidence is sparse or contradictory.
- Retain generated versus manually edited title/description provenance.
- Allow re-running summary generation after merging or changing the member selection, without rerunning embedding generation.

### Acceptance criteria

- Keyword rankings suppress boilerplate and repeated structural terms where appropriate.
- Analysts can see the phrases/evidence that influenced a description.
- A summary does not overwrite a manually edited title without an explicit confirmation.

---

## 12. Cross-cutting requirements

Every roadmap initiative should preserve these constraints:

### UI and architecture

- Use JavaFX/Java 25 conventions already present in Trinity.
- Keep the Similarity tab compact; use floating `LitPathPane` overlays for detailed review/configuration workflows.
- Manage overlays via `AppAsyncManager` and decouple panes through `HyperdriveEvent` or another existing event pattern.
- Avoid direct dependencies from floating panes to `HyperdrivePane`.
- Use no wildcard imports and avoid new inner classes wherever a top-level class is feasible.
- Apply `dialogstyles.css` to actual dialogs/Alerts, not to floating overlays in place of their normal styling.

### Processing and correctness

- Operate on original N-dimensional embeddings, not visualization projections.
- Keep UI operations on the FX thread and heavy calculations in background tasks.
- Preserve separate request batching/concurrency accounting for text and image workflows.
- Keep typed Jackson POJO binding; avoid ad hoc response parsers.
- Preserve record identity, source provenance, and correct response-to-input association.
- Treat invalid vectors and mixed dimensions as validation failures, not as arbitrary distances.
- Distinguish user-correctable model/configuration issues from service and programming errors.

### Notification policy

- Use the bottom-left `CommandTerminalEvent` status line for singleton status changes, partial outcomes, asynchronous errors, and completion summaries.
- Use a blocking Alert for preflight errors that require user correction before work can begin.
- Aggregate skipped/failed record counts rather than showing one dialog per record.
- Put detailed diagnostic information in SLF4J logs, avoiding unnecessary capture of prompts, documents, or model reasoning.

### Verification

- Add unit tests for pure calculation/parsing logic.
- Include integration checks for the event flow and overlay lifecycle where a workflow spans multiple panes.
- Test both valid and failure paths: blank/malformed content, unsupported model modality, malformed response, timeout, cancellation, stale responses, incompatible embeddings, and out-of-order request completion.
- State clearly whether a deliverable received source inspection, unit/smoke testing, full build verification, or interactive runtime testing.
- Use the latest user-supplied source tree as authoritative. Earlier ZIP snapshots may predate manually accepted corrections.

---

## 13. Recommended next work item

**Start with P1: Confidence-Aware Landmark Classification.**

It is the most direct extension of the now-verified Topic Discovery workflow. The first implementation should calculate and expose best-match distance and runner-up margin, identify ambiguous matches, and allow a configurable abstain/unclassified outcome while retaining existing nearest-landmark behavior as an option.

Follow that with **Topic Quality Diagnostics**, so analysts can evaluate both the quality of discovered clusters and the reliability of labels assigned from generated landmarks.
