package edu.jhuapl.trinity.data.graph.construction.source;

import edu.jhuapl.trinity.data.graph.GraphDirectedCollection;
import edu.jhuapl.trinity.data.graph.GraphEdge;
import edu.jhuapl.trinity.data.graph.GraphNode;
import edu.jhuapl.trinity.data.graph.GraphRelationship;
import edu.jhuapl.trinity.data.graph.semantics.GraphAssertionOrigin;
import edu.jhuapl.trinity.data.graph.semantics.GraphEvidence;
import edu.jhuapl.trinity.data.graph.semantics.GraphRelationshipSemantics;
import edu.jhuapl.trinity.data.graph.semantics.GraphRunProvenance;
import edu.jhuapl.trinity.data.graph.semantics.GraphSemanticValidator;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.BooleanSupplier;

/**
 * Builds a schema-v2 source-containment graph without trying to infer factual
 * taxonomy, entities or PDF headings. Source paths and passages are traceable
 * to the detached input snapshot; no UI objects or FeatureVectors are created.
 *
 * DOCUMENT -> PAGE -> PASSAGE (PDF)
 * DOCUMENT -> SECTION -> SUBSECTION -> PASSAGE (explicit Markdown ATX headings)
 * DOCUMENT -> PASSAGE (plain text, CSV/JSON-derived chunks with no raw span)
 *
 * Every edge is a directed, typed CONTAINS relationship in the source document's
 * named hierarchy. A chunk that overlaps two sections is conservatively placed
 * at their nearest containing parent, NOT assigned to an arbitrary heading.
 */
public final class SourceDocumentHierarchyBuilder {
    private SourceDocumentHierarchyBuilder() { }

    public static GraphDirectedCollection build(SourceDocumentSnapshot source) {
        return build(source, null, () -> false);
    }

    public static GraphDirectedCollection build(SourceDocumentSnapshot source, String graphId,
                                                BooleanSupplier cancelled) {
        Objects.requireNonNull(source, "source");
        if (cancelled == null) throw new IllegalArgumentException("cancelled supplier is required");
        checkCancelled(cancelled);
        GraphDirectedCollection graph = new GraphDirectedCollection();
        graph.setSchemaVersion(2);
        graph.setGraphId(graphId == null || graphId.isBlank()
            ? "source-hierarchy:" + source.documentId() + ":" + source.documentRevision()
            : graphId);
        GraphRunProvenance run = new GraphRunProvenance();
        run.setRunId(SourceDocumentIdentity.structuralId("run", source.documentId(),
            source.documentRevision(), source.extractionMethod(), new java.util.TreeMap<>(source.parameters()).toString()));
        run.setProducer("Trinity SourceDocumentHierarchyBuilder");
        run.setMethod("SOURCE_STRUCTURE_EXTRACTION");
        run.setSourceDocumentIds(List.of(source.documentId()));
        Map<String, Object> parameters = new LinkedHashMap<>(source.parameters());
        parameters.put("extractionMethod", source.extractionMethod());
        parameters.put("spanCoordinateSystem", SourceDocumentSnapshot.SPAN_COORDINATE_SYSTEM);
        run.setParameters(parameters);
        graph.setRunProvenance(run);

        String scope = "source:" + source.documentId();
        List<GraphNode> nodes = new ArrayList<>();
        List<GraphEdge> edges = new ArrayList<>();
        List<GraphEvidence> evidence = new ArrayList<>();
        long[] nodeIdentity = {1L};
        long[] relationshipIdentity = {1L};

        GraphNode document = node(nodeIdentity[0]++, source.documentId(), "DOCUMENT",
            source.sourceName());
        document.getProperties().put("sourceDocumentId", source.documentId());
        document.getProperties().put("sourceDocumentRevision", source.documentRevision());
        if (source.sourcePath() != null) document.getProperties().put("sourceFilePath", source.sourcePath());
        document.getProperties().put("extractionMethod", source.extractionMethod());
        nodes.add(document);

        Map<Integer, String> pageIds = new LinkedHashMap<>();
        for (SourceDocumentSnapshot.PageSpan page : source.pages()) {
            checkCancelled(cancelled);
            String id = SourceDocumentIdentity.structuralId("page", source.documentId(),
                source.documentRevision(), Integer.toString(page.pageNumber()));
            pageIds.put(page.pageNumber(), id);
            GraphNode pageNode = node(nodeIdentity[0]++, id, "PAGE", "Page " + page.pageNumber());
            pageNode.getProperties().put("pageNumber", page.pageNumber());
            putSpan(pageNode.getProperties(), page.startOffset(), page.endOffset());
            nodes.add(pageNode);
            edges.add(contains(source, relationshipIdentity[0]++, scope,
                source.documentId(), id, null));
        }

        List<Section> sections = isMarkdown(source.sourceName())
            ? parseMarkdownSections(source) : List.of();
        Map<String, Section> sectionById = new LinkedHashMap<>();
        for (Section section : sections) sectionById.put(section.id, section);
        for (Section section : sections) {
            checkCancelled(cancelled);
            GraphNode sectionNode = node(nodeIdentity[0]++, section.id, "SECTION", section.title);
            sectionNode.getProperties().put("headingLevel", section.level);
            putSpan(sectionNode.getProperties(), section.start, section.end);
            nodes.add(sectionNode);
            edges.add(contains(source, relationshipIdentity[0]++, scope,
                section.parentId == null ? source.documentId() : section.parentId,
                section.id, null));
        }

        for (SourceDocumentSnapshot.Passage passage : source.passages()) {
            if ((passage.chunkIndex() & 255) == 0) checkCancelled(cancelled);
            String parent = source.documentId();
            if (passage.pageNumber() != null) {
                parent = pageIds.get(passage.pageNumber());
                if (parent == null) throw new IllegalArgumentException("Missing source page");
            } else if (passage.hasExactSpan() && !sections.isEmpty()) {
                parent = containingSection(sections, sectionById, passage, source.documentId());
            }
            GraphNode passageNode = node(nodeIdentity[0]++, passage.passageId(), "PASSAGE",
                passage.text().substring(0, Math.min(100, passage.text().length())));
            passageNode.getProperties().put("text", passage.text());
            passageNode.getProperties().put("sourceDocumentId", source.documentId());
            passageNode.getProperties().put("sourceDocumentRevision", source.documentRevision());
            passageNode.getProperties().put("sourceChunkIndex", passage.chunkIndex());
            if (passage.label() != null) passageNode.getProperties().put("sourceLabel", passage.label());
            passageNode.getProperties().put("sourceMetadata", new LinkedHashMap<>(passage.metadata()));
            if (passage.hasExactSpan()) {
                putSpan(passageNode.getProperties(), passage.startOffset(), passage.endOffset());
            }
            if (passage.pageNumber() != null) {
                passageNode.getProperties().put("pageNumber", passage.pageNumber());
            }
            nodes.add(passageNode);

            GraphEvidence passageEvidence = new GraphEvidence();
            String evidenceId = SourceDocumentIdentity.structuralId("evidence", passage.passageId());
            passageEvidence.setEvidenceId(evidenceId);
            passageEvidence.setDocumentId(source.documentId());
            passageEvidence.setDocumentRevision(source.documentRevision());
            passageEvidence.setPassageId(passage.passageId());
            passageEvidence.setPageNumber(passage.pageNumber());
            if (!parent.equals(source.documentId()) && passage.pageNumber() == null) {
                passageEvidence.setSectionId(parent);
            }
            passageEvidence.setExtractionMethod(source.extractionMethod());
            if (passage.hasExactSpan()) {
                passageEvidence.setStartOffset(passage.startOffset().longValue());
                passageEvidence.setEndOffset(passage.endOffset().longValue());
                passageEvidence.setSpanCoordinateSystem(SourceDocumentSnapshot.SPAN_COORDINATE_SYSTEM);
                passageEvidence.setQuote(passage.text());
            }
            evidence.add(passageEvidence);
            edges.add(contains(source, relationshipIdentity[0]++, scope,
                parent, passage.passageId(), evidenceId));
        }
        graph.setNodes(nodes);
        graph.setEdges(edges);
        graph.setEvidenceRecords(evidence);
        checkCancelled(cancelled);
        GraphSemanticValidator.Result validation = GraphSemanticValidator.validate(graph,
            GraphSemanticValidator.Mode.STRICT_TYPED);
        if (!validation.isValid()) {
            throw new IllegalStateException("Generated source hierarchy failed semantic validation: "
                + validation.issues());
        }
        return graph;
    }

    private static GraphNode node(long identity, String id, String kind, String name) {
        GraphNode node = new GraphNode();
        node.setIdentity(identity);
        node.setEntityID(id);
        node.setNodeKind(kind);
        node.getLabels().add(kind.charAt(0) + kind.substring(1).toLowerCase(Locale.ROOT));
        node.getProperties().put("name", name);
        return node;
    }

    private static GraphEdge contains(SourceDocumentSnapshot source, long identity,
                                      String scope, String parent, String child,
                                      String evidenceId) {
        GraphEdge edge = new GraphEdge();
        edge.setEdgeId(SourceDocumentIdentity.structuralId("assertion", source.documentId(),
            source.documentRevision(), "CONTAINS", parent, child));
        edge.setStartID(parent);
        edge.setEndID(child);
        GraphRelationship relation = new GraphRelationship();
        relation.setIdentity(identity);
        relation.getLabels().add("CONTAINS");
        GraphRelationshipSemantics semantics = new GraphRelationshipSemantics();
        semantics.setPredicateId("CONTAINS");
        semantics.setOrigin(GraphAssertionOrigin.STRUCTURAL_IMPORTED);
        semantics.setHierarchyScopeId(scope);
        if (evidenceId != null) semantics.setEvidenceIds(List.of(evidenceId));
        relation.setSemantics(semantics);
        edge.setRelationship(relation);
        return edge;
    }

    private static void putSpan(Map<String, Object> properties, int start, int end) {
        properties.put("startOffset", start);
        properties.put("endOffset", end);
        properties.put("spanCoordinateSystem", SourceDocumentSnapshot.SPAN_COORDINATE_SYSTEM);
    }

    private static boolean isMarkdown(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.endsWith(".md") || lower.endsWith(".markdown");
    }

    private static final class Section {
        final String id;
        final String title;
        final String parentId;
        final int level;
        final int start;
        int end;

        Section(String id, String title, String parentId, int level, int start, int end) {
            this.id = id;
            this.title = title;
            this.parentId = parentId;
            this.level = level;
            this.start = start;
            this.end = end;
        }
    }

    /** Only recognizes explicit Markdown ATX headings. Never invents PDF headings. */
    private static List<Section> parseMarkdownSections(SourceDocumentSnapshot source) {
        List<Section> sections = new ArrayList<>();
        Deque<Section> stack = new ArrayDeque<>();
        String content = source.canonicalText();
        int start = 0;
        while (start < content.length()) {
            int end = start;
            while (end < content.length() && content.charAt(end) != '\n'
                    && content.charAt(end) != '\r') end++;
            String line = content.substring(start, end);
            int n = 0;
            while (n < line.length() && line.charAt(n) == '#') n++;
            if (n >= 1 && n <= 6 && n < line.length()
                    && (line.charAt(n) == ' ' || line.charAt(n) == '\t')) {
                String title = line.substring(n).trim().replaceFirst("\\s+#+\\s*$", "").trim();
                if (!title.isEmpty()) {
                    while (!stack.isEmpty() && stack.peek().level >= n) {
                        stack.pop().end = start;
                    }
                    String parent = stack.isEmpty() ? null : stack.peek().id;
                    String id = SourceDocumentIdentity.structuralId("section", source.documentId(),
                        source.documentRevision(), Integer.toString(start), title);
                    Section section = new Section(id, title, parent, n, start, content.length());
                    sections.add(section);
                    stack.push(section);
                }
            }
            if (end == content.length()) break;
            if (content.charAt(end) == '\r' && end + 1 < content.length()
                    && content.charAt(end + 1) == '\n') end++;
            start = end + 1;
        }
        return sections;
    }

    /** Select deepest enclosing section; crossing heading boundaries backs off to a parent. */
    private static String containingSection(List<Section> sections,
                                            Map<String, Section> sectionById,
                                            SourceDocumentSnapshot.Passage passage,
                                            String documentId) {
        int low = 0, high = sections.size();
        while (low < high) {
            int mid = (low + high) >>> 1;
            if (sections.get(mid).start <= passage.startOffset()) low = mid + 1;
            else high = mid;
        }
        // Walk upward from the most recently opened heading until its range
        // contains the WHOLE chunk (which may straddle a child heading).
        Section candidate = low == 0 ? null : sections.get(low - 1);
        while (candidate != null) {
            if (candidate.start <= passage.startOffset()
                    && candidate.end >= passage.endOffset()) return candidate.id;
            candidate = sectionById.get(candidate.parentId);
        }
        return documentId;
    }

    private static void checkCancelled(BooleanSupplier cancelled) {
        if (Thread.currentThread().isInterrupted() || cancelled.getAsBoolean()) {
            throw new java.util.concurrent.CancellationException("Source hierarchy construction cancelled");
        }
    }
}
