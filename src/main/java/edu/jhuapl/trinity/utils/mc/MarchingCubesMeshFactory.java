/* Copyright (C) 2021 - 2024 Sean Phillips */

package edu.jhuapl.trinity.utils.mc;

import javafx.scene.shape.TriangleMesh;
import org.fxyz3d.geometry.Vector3D;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.DoubleBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Factory for creating meshes out of a scalar field using the marching cubes algorithm. The reference is the back bottom left point, which is locally the point (0, 0, 0).
 */
public class MarchingCubesMeshFactory {
    private static final Logger LOG = LoggerFactory.getLogger(MarchingCubesMeshFactory.class);
    int SHARED_VERTICES_PER_CUBE = 8;
    int INDEX_BUFFER_COMPONENT_COUNT = 3;
    int NORMAL_BUFFER_COMPONENT_COUNT = 3;
    int VERTICES_PER_TRIANGLE = 3;
    private float[][][] m_scalarField;
    private float m_isoLevel;
    private float m_cubeDiameter;
    private int m_gridLengthX;
    private int m_gridLengthY;
    private int m_gridLengthZ;
    private float[] m_cubeScalars;
    //    private Vector3f m_origin;
    private Vector3D m_origin;
    private List<Vector3D> m_vertrexList;

    /**
     * Constructs a new MarchingCubesMeshFactory for generating meshes out of a scalar field with the marching cubes algorithm.
     *
     * @param scalarField  Contains the density of each position.
     * @param isoLevel     The minimum density needed for a position to be considered solid.
     * @param cubeDiameter The diameter of a single voxel.
     */
    public MarchingCubesMeshFactory(float[][][] scalarField, float isoLevel, float cubeDiameter) {
        this.m_scalarField = scalarField;
        this.m_isoLevel = isoLevel;
        this.m_cubeDiameter = cubeDiameter;
        this.m_gridLengthX = scalarField.length;
        this.m_gridLengthY = scalarField[0].length;
        this.m_gridLengthZ = scalarField[0][0].length;
        this.m_origin = computeCenterPoint();
    }

    /**
     * Constructs a new MarchingCubesMeshFactory for generating meshes out of a scalar field with the marching cubes algorithm.
     *
     * @param scalarField  Contains the density of each position.
     * @param isoLevel     The minimum density needed for a position to be considered solid.
     * @param origin       The local origin for all vertices of the generated mesh.
     * @param cubeDiameter The diameter of a single voxel.
     */
    public MarchingCubesMeshFactory(float[][][] scalarField, float isoLevel, Vector3D origin, float cubeDiameter) {
        this.m_scalarField = scalarField;
        this.m_isoLevel = isoLevel;
        this.m_cubeDiameter = cubeDiameter;
        this.m_gridLengthX = scalarField.length;
        this.m_gridLengthY = scalarField[0].length;
        this.m_gridLengthZ = scalarField[0][0].length;
        this.m_origin = origin;
    }

    public TriangleMesh createMesh() {
        TriangleMesh mesh = new TriangleMesh();
//TODO SMP
        DoubleBuffer positionBuffer = createPositionBuffer();
        double[] vertices = positionBuffer.array();
        for (int i = 0; i < vertices.length; i += VERTICES_PER_TRIANGLE) {
            mesh.getPoints().addAll(
                Double.valueOf(vertices[i]).floatValue(),
                Double.valueOf(vertices[i + 1]).floatValue(),
                Double.valueOf(vertices[i + 2]).floatValue()
            );
        }
        int totalFaces = mesh.getPoints().size() / 3;
        for (int faceIndex = 0; faceIndex < totalFaces - 1; faceIndex++) {
            if (faceIndex + 2 > mesh.getPoints().size())
                LOG.info("wft...");
            mesh.getFaces().addAll(
                faceIndex, faceIndex + 1, faceIndex + 2,
                faceIndex + 2, faceIndex + 1, faceIndex
            );
        }

        mesh.getTexCoords().addAll(0, 0);
//        IntBuffer indexBuffer = createIndexBuffer();
//        for(int d : indexBuffer.array()){
//            mesh.getFaces().addAll(d);
//        }

//        mesh.setVertexFormat(VertexFormat.POINT_NORMAL_TEXCOORD);


//        DoubleBuffer normalBuffer = createNormalBuffer();
//        for(Double d : normalBuffer.array()){
//            mesh.getNormals().addAll(d.floatValue());
//        }

//        mesh.updateBound();

        return mesh;
    }

    private DoubleBuffer createNormalBuffer() {
        //DoubleBuffer normalBuffer = (DoubleBuffer) VertexBuffer.createBuffer(Format.Float, NORMAL_BUFFER_COMPONENT_COUNT, m_vertrexList.size());
        DoubleBuffer normalBuffer = DoubleBuffer.allocate(m_vertrexList.size() * NORMAL_BUFFER_COMPONENT_COUNT);

        for (int i = VERTICES_PER_TRIANGLE - 1; i < m_vertrexList.size(); i += VERTICES_PER_TRIANGLE) {
            Vector3D normal = computeTriangleNormal(m_vertrexList.get(i - 2), m_vertrexList.get(i - 1), m_vertrexList.get(i));

            for (int j = 0; j < NORMAL_BUFFER_COMPONENT_COUNT; ++j)
                normalBuffer.put(normal.x).put(normal.y).put(normal.z);
        }

        return normalBuffer;
    }

    private IntBuffer createIndexBuffer() {
//        IntBuffer indexBuffer = (IntBuffer) VertexBuffer.createBuffer(Format.Int, INDEX_BUFFER_COMPONENT_COUNT,
//                m_vertrexList.size() / INDEX_BUFFER_COMPONENT_COUNT);
        IntBuffer indexBuffer = IntBuffer.allocate(m_vertrexList.size() / INDEX_BUFFER_COMPONENT_COUNT);

        for (int vertexIndex = 0; vertexIndex < m_vertrexList.size(); ++vertexIndex)
            indexBuffer.put(vertexIndex);

        return indexBuffer;
    }

    private DoubleBuffer createPositionBuffer() {
        m_vertrexList = new ArrayList<>();

        for (int x = -1; x <= m_gridLengthX; ++x)
            for (int y = -1; y <= m_gridLengthY; ++y)
                for (int z = -1; z <= m_gridLengthZ; ++z) {
//                    Vector3D[] cubeVertices = new Vector3D[MeshBufferUtils.SHARED_VERTICES_PER_CUBE];
                    Vector3D[] cubeVertices = new Vector3D[SHARED_VERTICES_PER_CUBE];
                    int cubeIndex = computeCubeIndex(cubeVertices, x, y, z);
                    int edgeBitField = MarchingCubesTables.EDGE_TABLE[cubeIndex];
                    if (edgeBitField == 0)
                        continue;

                    Vector3D[] mcVertices = computeMCVertices(cubeVertices, edgeBitField, m_isoLevel);
                    addVerticesToList(m_vertrexList, mcVertices, cubeIndex);
                }

        return addVerticesToPositionBuffer();
    }

    private DoubleBuffer addVerticesToPositionBuffer() {
//        DoubleBuffer positionBuffer = (DoubleBuffer) VertexBuffer.createBuffer(Format.Float, MeshBufferUtils.VERTEX_BUFFER_COMPONENT_COUNT, m_vertrexList.size());
        DoubleBuffer positionBuffer = DoubleBuffer.allocate(m_vertrexList.size() * VERTICES_PER_TRIANGLE);

        for (int i = 0; i < m_vertrexList.size(); ++i) {
            Vector3D position = m_vertrexList.get(i);
            positionBuffer.put(position.x).put(position.y).put(position.z);
        }

        return positionBuffer.flip();
    }

    /**
     * Add the generated vertices by the marching cubes algorithm to a list. The added vertices are modified so that they respect the origin.
     *
     * @param vertrexList The list where to add the marching cubes vertices.
     * @param mcVertices  The marching cubes vertices.
     * @param cubeIndex   The cubeIndex.
     */
    private void addVerticesToList(List<Vector3D> vertrexList, Vector3D[] mcVertices, int cubeIndex) {
        int vertexCount = MarchingCubesTables.TRIANGLE_TABLE[cubeIndex].length;
        for (int i = 0; i < vertexCount; ++i)
            vertrexList.add(mcVertices[MarchingCubesTables.TRIANGLE_TABLE[cubeIndex][i]].add(m_origin));
    }

    /**
     * Computes the marching cubes vertices. Those are the lerped vertices that can later be used to form triangles.
     *
     * @param cubeVertices The vertices of a cube, i.e. the 8 corners.
     * @param edgeBitField The bit field representing all the edges that should be drawn.
     * @param isoLevel     The minimum density needed for a position to be considered solid.
     * @return The lerped vertices of a cube to form the marching cubes shape.
     */
    private Vector3D[] computeMCVertices(Vector3D[] cubeVertices, int edgeBitField, float isoLevel) {
        Vector3D[] lerpedVertices = new Vector3D[MarchingCubesTables.EDGE_BITS];

        for (int i = 0; i < MarchingCubesTables.EDGE_BITS; ++i) {
            if ((edgeBitField & (1 << i)) != 0) {
                int edgeFirstIndex = MarchingCubesTables.EDGE_FIRST_VERTEX[i];
                int edgetSecondIndex = MarchingCubesTables.EDGE_SECOND_VERTEX[i];

                lerpedVertices[i] = MCLerp(cubeVertices[edgeFirstIndex], cubeVertices[edgetSecondIndex], m_cubeScalars[edgeFirstIndex], m_cubeScalars[edgetSecondIndex]);
            }
        }

        return lerpedVertices;
    }

    static double[] lerp(double[] vec1, double[] vec2, double alpha) {
        return new double[]{vec1[0] + (vec2[0] - vec1[0]) * alpha,
            vec1[1] + (vec2[1] - vec1[1]) * alpha,
            vec1[2] + (vec2[2] - vec1[2]) * alpha};
    }

    /**
     * Lerps two vertices of a cube along their shared designed edge according to their densities.
     *
     * @param firstVertex  The edge's first vertex.
     * @param secondVertex The edge's second vertex.
     * @param firstScalar  The first vertex's density.
     * @param secondScalar The second vertex's density.
     * @return The lerped resulting vertex along the edge.
     */

    private Vector3D MCLerp(Vector3D firstVertex, Vector3D secondVertex, float firstScalar, float secondScalar) {
        if (Math.abs(m_isoLevel - firstScalar) < Math.ulp(1f))
            return firstVertex;
        if (Math.abs(m_isoLevel - secondScalar) < Math.ulp(1f))
            return secondVertex;
        if (Math.abs(firstScalar - secondScalar) < Math.ulp(1f))
            return firstVertex;

        float lerpFactor = (m_isoLevel - firstScalar) / (secondScalar - firstScalar);

//        return firstVertex.clone().interpolateLocal(secondVertex, lerpFactor);
        return new Vector3D(lerp(firstVertex.toDoubleArray(), secondVertex.toDoubleArray(), lerpFactor));
    }

    /**
     * Computes the cubeIndex, which represents the adjacent voxels' densities.
     *
     * @param cubeVertices The 8 corners of a cube.
     * @param indexX       The X position of the marching cube in the grid.
     * @param indexY       The Y position of the marching cube in the grid.
     * @param indexZ       The Z position of the marching cube in the grid.
     * @return The cubeIndex.
     */
    private int computeCubeIndex(Vector3D[] cubeVertices, int indexX, int indexY, int indexZ) {
        m_cubeScalars = new float[SHARED_VERTICES_PER_CUBE];
        final int edgeLength = 2;
        int cubeVertexIndex = 0;
        int cubeIndex = 0;
        int cubeIndexRHS = 1;

        /*- Vertex indices
                        4  ___________________  5
                          /|                 /|
                         / |                / |
                        /  |               /  |
                   7   /___|______________/6  |
                      |    |              |   |
                      |    |              |   |
                      |  0 |______________|___| 1
                      |   /               |   /
                      |  /                |  /
                      | /                 | /
                      |/__________________|/
                     3                     2
        */

        for (int y = 0; y < edgeLength; ++y)
            for (int z = 0; z < edgeLength; ++z)
                for (int x = z % edgeLength; x >= 0 && x < edgeLength; x += (z == 0 ? 1 : -1)) {
                    cubeVertices[cubeVertexIndex] = new Vector3D((indexX + x) * m_cubeDiameter, (indexY + y) * m_cubeDiameter, (indexZ + z) * m_cubeDiameter);
                    m_cubeScalars[cubeVertexIndex++] = queryGridScalar(indexX + x, indexY + y, indexZ + z);

                    if (queryGridIsSolid(indexX + x, indexY + y, indexZ + z))
                        cubeIndex |= cubeIndexRHS;

                    cubeIndexRHS <<= 1;
                }

        return cubeIndex;
    }

    /**
     * Queries if the grid is dense enough to be considered solid at the give (x, y, z) point.
     *
     * @param x The index on the X axis.
     * @param y The index on the Y axis.
     * @param z The index on the Z axis.
     * @return If the grid is solid or empty at the given point.
     */
    private boolean queryGridIsSolid(int x, int y, int z) {
        return isScalarSolid(queryGridScalar(x, y, z));
    }

    /**
     * Queries the grid scalar at the given point and manages the boundaries, i.e. it's ok if x = -1 or is bigger than the gridLengthX.
     *
     * @param x The scalar X position in the grid.
     * @param y The scalar X position in the grid.
     * @param z The scalar X position in the grid.
     * @return The grid scalar at the (x, y, z) position.
     */
    private float queryGridScalar(int x, int y, int z) {
        if (x >= 0 && x < m_scalarField.length && y >= 0 && y < m_scalarField[0].length && z >= 0 && z < m_scalarField[0][0].length)
            return m_scalarField[x][y][z];
        else
            return 0f;
    }

    public Vector3D computeCenterPoint() {
        return new Vector3D((-m_gridLengthX * m_cubeDiameter + m_cubeDiameter) / 2, (-m_gridLengthY * m_cubeDiameter + m_cubeDiameter) / 2, (-m_gridLengthZ * m_cubeDiameter + m_cubeDiameter) / 2);
    }

    public static Vector3D computeTriangleNormal(Vector3D[] vertices) {
        return computeTriangleNormal(vertices[0], vertices[1], vertices[2]);
    }

    public static Vector3D computeTriangleNormal(Vector3D p1, Vector3D p2, Vector3D p3) {
//        return p2.subtract(p1).crossLocal(p3.subtract(p1)).normalizeLocal();
        Vector3D cross = p2.sub(p1).crossProduct(p3.sub(p1));
        cross.normalize();
        return cross;
    }

    private boolean isScalarSolid(float scalar) {
        return scalar > m_isoLevel;
    }
}
