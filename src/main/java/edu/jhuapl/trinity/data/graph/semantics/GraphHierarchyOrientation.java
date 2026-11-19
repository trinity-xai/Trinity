package edu.jhuapl.trinity.data.graph.semantics;

/**
 * How a directed graph edge maps to child -> parent for hierarchy traversal.
 * This is explicit; arbitrary subject/object role strings are not interpreted.
 */
public enum GraphHierarchyOrientation {
    /** startID identifies the child, endID identifies the parent (SUBCLASS_OF). */
    SUBJECT_IS_CHILD,
    /** startID identifies the parent, endID identifies the child (CONTAINS). */
    SUBJECT_IS_PARENT
}
