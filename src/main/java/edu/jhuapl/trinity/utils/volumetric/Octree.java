/* Copyright (C) 2021 - 2024 Sean Phillips */

package edu.jhuapl.trinity.utils.volumetric;

import edu.jhuapl.trinity.utils.volumetric.VolumeUtils.Adjacency;
import javafx.geometry.Point3D;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import static java.lang.Math.pow;
import static java.lang.Math.sqrt;


/**
 * An octree recursively divide a box 3d space into eight octants and can accelerate
 * the process of searching nearest neighbors of points.
 */
public class Octree {
    private static final Logger LOG = LoggerFactory.getLogger(Octree.class);
    /**
     * the root node of the octree,
     * the size of root node is the bounding box of point cloud
     **/
    protected OctreeNode root = null;

    protected List<Point3D> points = null;

    /**
     * used to find an octree node by its index
     * the index of an octree node is generated according to its spatial position
     */
    protected Map<Long, OctreeNode> octreeIndices = new HashMap<>();

    /**
     * the max depth of this tree,
     * theoretically it shall be less than 21, because every three bits in a Long-type
     * octree-node index is used to locate its position of siblings.
     **/
    protected static final int MAX_DEPTH = 10;

    /**
     * an octree cell contains at most 100 points
     **/
    private int maxPointsPerNode = 100;

    /**
     * build spatial index for point cloud
     * note that the length double array in List points must be 3
     *
     * @param points the point cloud
     */
    public void buildIndex(List<Point3D> points) {
        if (points.size() < 1) {
            LOG.error("Warning: input for buildIndex() is an empty list.");
            return;
        }

        this.octreeIndices.clear();
        this.points = points;

        createRootNode();
        createOctree(0, this.root);
    }


    /**
     * If you want to acquire the k-nearest neighbors of a certain point p, call this function,
     * octree will decrease the time cost
     *
     * @param k     the number of nearest neighbors
     * @param index the index of point p
     * @return the indices of nearest neighbors
     */
    public int[] searchNearestNeighbors(int k, int index) {
        return searchNearestNeighbors(k, points.get(index));
    }

    private Comparator<Integer> distanceComparator(final Point3D point, boolean smallFirst) {
        if (smallFirst)
            return new Comparator<Integer>() {
                @Override
                public int compare(Integer pointIndex1, Integer pointIndex2) {
                    Point3D p1 = points.get(pointIndex1);
                    Point3D p2 = points.get(pointIndex2);
                    return Double.compare(p1.distance(point), p2.distance(point));
                }
            };
        return new Comparator<Integer>() {
            @Override
            public int compare(Integer pointIndex1, Integer pointIndex2) {
                Point3D p1 = points.get(pointIndex1);
                Point3D p2 = points.get(pointIndex2);
                return -Double.compare(p1.distance(point), p2.distance(point));
            }
        };
    }

    private Long indexOfNearestCell(final Point3D point) {
        long result = locateOctreeNode(root, point);
        if (result == -1) {
            PriorityQueue<Long> queue = new PriorityQueue<>(octreeIndices.size(), new Comparator<Long>() {
                @Override
                public int compare(Long o1, Long o2) {
                    OctreeNode node1 = octreeIndices.get(o1);
                    OctreeNode node2 = octreeIndices.get(o2);
                    Double distance1 = point.distance(node1.getCenter());
                    Double distance2 = point.distance(node2.getCenter());
                    return distance1.compareTo(distance2);
                }
            });
            queue.addAll(octreeIndices.keySet());
            result = queue.poll();
        }
        return result;
    }

    /**
     * search k nearest neighbors for the point
     *
     * @param k     the number of nearest neighbors, {@literal 0 <= k < points.size()}
     * @param point the point, non-null
     * @return an array of indices of neighboring point, the length is k,
     * @throws IllegalStateException    if previously forget to call buildIndex()
     * @throws IllegalArgumentException if size of points is less than k + 1
     */
    public int[] searchNearestNeighbors(int k, final Point3D point) {
        if (points == null) throw new IllegalStateException("Octree.buildIndex() must be called before searchNearestNeighbors.");
        if (k >= this.points.size() || k < 0) throw new IllegalArgumentException("number of nearest neighbors is larger than data size");
        if (!VolumeUtils.validPoint(point)) throw new IllegalArgumentException("The coordinates of given point is invalid");
        if (k == 0) return new int[]{};
        Comparator<Integer> comparator = distanceComparator(point, false);

        long leafNodeIndex = indexOfNearestCell(point);

        PriorityQueue<Integer> queue = new PriorityQueue<>(k + 1, comparator);
        Set<Point3D> set = new HashSet<>(k * 2);
        Set<Long> visitedBoxes = new HashSet<>();
        double leafSize = octreeIndices.get(leafNodeIndex).getxExtent();

        Sphere searchRange = new Sphere(point, leafSize);
        set.add(point);

        while (queue.size() < k) {
            Set<Long> candidates = new HashSet<>();
//            determineCandidatesWithinRadius(currentSearchRadius, point, candidates);
            determineCandidatesWithinRadius(searchRange.getRadius(), searchRange.getCenter(), candidates);
            for (Long newNode : candidates) {
                if (visitedBoxes.contains(newNode)) continue;
                if (VolumeUtils.contains(searchRange, octreeIndices.get(newNode))) visitedBoxes.add(newNode);
                for (int index : octreeIndices.get(newNode).indices) {
                    if (set.contains(points.get(index))) continue;
                    if (points.get(index).distance(point) <= searchRange.getRadius()) {
                        queue.add(index);
                        set.add(points.get(index));
                    }
                    if (queue.size() > k) queue.poll();
                }
//                queue.addAll(octreeIndices.get(newNode).indices);
            }
//            Collections.sort(queue, comparator);
//            while (queue.size() > k) queue.remove(queue.size() - 1);
            searchRange.setRadius(searchRange.getRadius() + leafSize);
        }

        int[] indices = new int[k];
        for (int i = 0; i < k; i++) {
//            indices[i] = queue.get(i);
            indices[i] = queue.poll();
        }
        VolumeUtils.reverse(indices);
        return indices;
    }

    /**
     * determine bounding box of data points, expand the box to make it cubic
     */
    private void createRootNode() {
        Box bbox = BoundingBox.of(points);
        double maxExtent = VolumeUtils.max(bbox.getxExtent(), bbox.getyExtent(), bbox.getzExtent());
        this.root = new OctreeNode(bbox.getCenter(), maxExtent, 0);
        this.root.indices = new ArrayList<>(points.size());
        this.root.indices.addAll(VolumeUtils.incrementalIntegerList(points.size()));
    }

    /**
     * partition the space recursively
     *
     * @param currentDepth the depth of current octree node
     * @param currentNode  current octree node
     */
    protected void createOctree(int currentDepth, OctreeNode currentNode) {
        if (currentNode.indices.size() < 1) return;
        if (currentNode.indices.size() <= maxPointsPerNode || currentDepth >= MAX_DEPTH) {
            this.octreeIndices.put(currentNode.index, currentNode);
            return;
        }
        currentNode.children = new OctreeNode[8];
        int cnt = 0;
        for (int i : new int[]{-1, 1}) {
            for (int j : new int[]{-1, 1}) {
                for (int k : new int[]{-1, 1}) {
                    long index = (long) (((i + 1) * 2 + (j + 1) * 1 + (k + 1) / 2));
//                    index <<= (currentDepth - 1) * 3;
                    index = currentNode.index | (index << (3 * currentDepth + 3));
                    double length = currentNode.getxExtent(); // xExtent == yExtent == zExtent
                    Point3D center = new Point3D(
                        currentNode.getCenter().getX() + i * length / 2,
                        currentNode.getCenter().getY() + j * length / 2,
                        currentNode.getCenter().getZ() + k * length / 2);
                    OctreeNode node = new OctreeNode(center, length / 2, currentDepth + 1);
                    currentNode.children[cnt] = node;
                    node.index = index;
                    node.indices = new ArrayList<>(currentNode.indices.size() / 8 + 10);
                    cnt += 1;
                }
            }
        }
        for (int index : currentNode.indices) {
            Point3D point = points.get(index);
            if (!VolumeUtils.validPoint(point)) continue;
            Point3D center = currentNode.getCenter();
            int xi = point.getX() < center.getX() ? 0 : 1;
            int yj = point.getY() < center.getY() ? 0 : 1;
            int zk = point.getZ() < center.getZ() ? 0 : 1;
            int childIndex = xi * 4 + yj * 2 + zk * 1;
            currentNode.children[childIndex].indices.add(index);
        }
        currentNode.indices = null;
        for (OctreeNode node : currentNode.children) {
            createOctree(currentDepth + 1, node);
        }
    }


    /**
     * find the index of octree node in which the target point is located
     *
     * @param node  the root octree node
     * @param point the target point
     * @return the index of leaf node
     */
    protected Long locateOctreeNode(OctreeNode node, Point3D point) {
        if (node.children == null) {
            if (node.indices.size() > 0) {
                return node.index;
            } else {
                return -1L;
//                throw new IllegalStateException("Search a point exceeding octree bounds.");
            }
        } else {
            int xi = point.getX() < node.getCenter().getX() ? 0 : 1;
            int yj = point.getY() < node.getCenter().getY() ? 0 : 1;
            int zk = point.getZ() < node.getCenter().getZ() ? 0 : 1;
            int childIndex = xi * 4 + yj * 2 + zk * 1;
            return locateOctreeNode(node.children[childIndex], point);
        }
    }


    private PriorityQueue<Integer> searchNeighborsInNodes(List<Long> candidateLeaves, final Point3D point) {
        int capacity = 0;
        for (long leafIndex : candidateLeaves) capacity += this.octreeIndices.get(leafIndex).indices.size();
        PriorityQueue<Integer> queue = new PriorityQueue<>(capacity, new Comparator<Integer>() {
            @Override
            public int compare(Integer pointIndex1, Integer pointIndex2) {
                Point3D p1 = points.get(pointIndex1);
                Point3D p2 = points.get(pointIndex2);
                return Double.compare(p1.distance(point), p2.distance(point));
            }
        });
        for (long leafIndex : candidateLeaves) {
            queue.addAll(this.octreeIndices.get(leafIndex).indices);
        }
        return queue;
    }

    /**
     * search all neighboring points of specified point within distance
     *
     * @param point  the point
     * @param radius the distance
     * @return a List of indices of neighboring points
     */
    public List<Integer> searchAllNeighborsWithinDistance(Point3D point, double radius) {
        List<Integer> neighborIndices = new ArrayList<>();
        List<Long> candidateLeaves = new ArrayList<>();
        determineCandidatesWithinRadius(radius, point, candidateLeaves);

        PriorityQueue<Integer> queue = searchNeighborsInNodes(candidateLeaves, point);

        while (queue.size() > 0) {
            Integer nextIndex = queue.poll();
            Point3D neighboringPoint = points.get(nextIndex);
            if (point.distance(neighboringPoint) >= radius) {
                break;
            } else {
                neighborIndices.add(nextIndex);
            }
        }
        return neighborIndices;

    }

    /**
     * search all neighboring points of the point with specified index within distance
     *
     * @param index  the index of a point
     * @param radius radius of neighborhood
     * @return indices of neighboring points of this point
     */
    public List<Integer> searchAllNeighborsWithinDistance(int index, double radius) {
        return searchAllNeighborsWithinDistance(points.get(index), radius);
    }

    private void determineCandidatesWithinRadius(double radius, Point3D point, Collection<Long> candidates) {
        Sphere sphere = new Sphere(point, radius);
        // ===========================================
        // all octree nodes that intersects with sphere will be added into queue
        List<OctreeNode> visitingQueue = new ArrayList<>();
        if (VolumeUtils.intersect(root, sphere)) visitingQueue.add(root);
        int currentVisit = 0;
        for (; currentVisit < visitingQueue.size(); currentVisit++) {
            OctreeNode visiting = visitingQueue.get(currentVisit);
            if (visiting.isLeaf()) {
                if (octreeIndices.get(visiting.index) == null) continue;
                candidates.add(visiting.index);
            } else {
                for (OctreeNode child : visiting.children) {
                    if (VolumeUtils.intersect(child, sphere)) {
                        visitingQueue.add(child);
                    }
                }
            }
        }
    }

    /**
     * search adjacent nodes of an octree node
     *
     * @param nodeIndex the index of octree node
     * @param adjacency see {@link Adjacency}
     * @return the list of adjacent octree nodes
     */
    public List<OctreeNode> adjacentNodes(Long nodeIndex, Adjacency adjacency) {
        List<OctreeNode> result = new ArrayList<>();
        OctreeNode node = octreeIndices.get(nodeIndex);
        if (points == null) throw new IllegalStateException("Must call buildIndex() before searching adjacent nodes.");
        if (node == null) throw new IllegalArgumentException("Cannot find the octree node.");
        // compute half of diagonal length of the box
        double threshold = pow(node.getxExtent(), 2) + pow(node.getyExtent(), 2) + pow(node.getzExtent(), 2);
        threshold = sqrt(threshold);
        List<Long> candidates = new ArrayList<>();
        determineCandidatesWithinRadius(threshold + 1E-5, node.getCenter(), candidates);
        for (Long leafIndex : candidates) {
            OctreeNode leafNode = octreeIndices.get(leafIndex);
            double distance = leafNode.getCenter().distance(node.getCenter());
            double faceThreshold = leafNode.getxExtent() + node.getxExtent();
            double edgeThreshold = (leafNode.getxExtent() + node.getxExtent()) * sqrt(2.0);
            double vertexThreshold = (leafNode.getxExtent() + node.getxExtent()) * sqrt(3.0);
            switch (adjacency) {
                case FACE:
                    if (distance >= faceThreshold && distance < edgeThreshold - 1E-6) {
                        result.add(leafNode);
                    }
                    break;
                case EDGE:
                    if (distance >= faceThreshold && distance < vertexThreshold - 1E-6) {
                        result.add(leafNode);
                    }
                    break;
                case VERTEX:
                    if (distance >= vertexThreshold - 1E-6 && distance <= vertexThreshold + 1E-6) {
                        result.add(leafNode);
                    }
                    break;
            }
        }
        return result;
    }

    public int getMaxPointsPerNode() {
        return this.maxPointsPerNode;
    }

    public void setMaxPointsPerNode(int m) {
        this.maxPointsPerNode = m;
    }

    /**
     * The octree node in the 3d space.
     * Each node can have eight children nodes.
     * The leaf node has the indices of points that is located in this cell.
     */
    public class OctreeNode extends Box {

        /**
         * default value is root index
         * the index is generated in createOctree()
         **/
        Long index = 0L;

        /**
         * an octree node holds the indices of 3d points in the List
         * in a non-leaf node, field indices is null
         **/
        List<Integer> indices = null;

        /**
         * in a non-leaf node, field indices is null
         **/
        OctreeNode[] children = null;

        int depth = 0;

        public OctreeNode(Point3D center, double length, int depth) {
            this.center = new Point3D(center.getX(), center.getY(), center.getZ());
            this.xExtent = length;
            this.yExtent = length;
            this.zExtent = length;
            this.depth = depth;
        }

        public Long getIndex() {
            return index;
        }

        public void setIndex(long i) {
            this.index = i;
        }

        public List<Integer> getIndices() {
            return indices;
        }

        public void setIndices(List<Integer> indices) {
            this.indices = indices;
        }

        public OctreeNode[] getChildren() {
            return children;
        }

        public void setChildren(OctreeNode[] nodes) {
            this.children = nodes;
        }

        public int getDepth() {
            return depth;
        }

        public boolean isLeaf() {
            return children == null;
        }
    }

}
