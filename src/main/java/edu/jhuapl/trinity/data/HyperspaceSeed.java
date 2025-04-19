package edu.jhuapl.trinity.data;

import java.util.function.Function;

/**
 * @author Sean Phillips
 */
public class HyperspaceSeed {
    public String label = "";
    public Integer layer = null;
    public Double score = null;
    public Double pfa = null;
    public boolean visible = true;
    public int x, y, z; //coordinate indices
    public int xDir, yDir, zDir; //coordinate indices
    public double[] vector;

    public HyperspaceSeed(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.xDir = 0;
        this.yDir = 0;
        this.zDir = 0;
    }

    public HyperspaceSeed(int x, int y, int z, int xDir, int yDir, int zDir) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.xDir = xDir;
        this.yDir = yDir;
        this.zDir = zDir;
    }

    public HyperspaceSeed(int x, int y, int z, int xDir, int yDir, int zDir, double[] vector) {
        this(x, y, z);
        setVector(vector);
        this.xDir = 0;
        this.yDir = 0;
        this.zDir = 0;
        this.xDir = xDir;
        this.yDir = yDir;
        this.zDir = zDir;
    }

    public void setVector(double[] vector) {
        this.vector = new double[vector.length];
        System.arraycopy(vector, 0, this.vector, 0, this.vector.length);
    }

    public static Function<HyperspaceSeed, String> mapToString = (seed) -> {
        String seedString = seed.x + ", " + seed.y + ", "
            + seed.z;
        if (null != seed.vector)
            for (int i = 0; i < seed.vector.length; i++)
                seedString.concat(", " + seed.vector[i]);
        return seedString;
    };
}
