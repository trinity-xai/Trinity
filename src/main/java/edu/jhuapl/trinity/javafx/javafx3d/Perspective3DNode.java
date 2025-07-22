package edu.jhuapl.trinity.javafx.javafx3d;

import edu.jhuapl.trinity.data.FactorLabel;
import edu.jhuapl.trinity.data.FeatureLayer;
import edu.jhuapl.trinity.data.HyperspaceSeed;
import edu.jhuapl.trinity.javafx.components.ColorMap;
import edu.jhuapl.trinity.javafx.events.ColorMapEvent.COLOR_MAP;
import edu.jhuapl.trinity.javafx.events.HyperspaceEvent.COLOR_MODE;
import edu.jhuapl.trinity.utils.DataUtils;
import edu.jhuapl.trinity.utils.Utils;
import javafx.scene.paint.Color;

import java.util.Comparator;

/**
 * @author Sean Phillips
 * Six dimensional Perspective Data object
 */
public class Perspective3DNode {

    public HyperspaceSeed factorAnalysisSeed;
    public double xCoord;
    public double yCoord;
    public double zCoord;
    public double xDirCoord;
    public double yDirCoord;
    public double zDirCoord;
    public Color nodeColor = null;
    public boolean visible = true;

    public Perspective3DNode(double xParam, double yParam, double zParam,
                             double xDirParam, double yDirParam, double zDirParam, HyperspaceSeed factorAnalysisSeed) {
        this(xParam, yParam, zParam, xDirParam, yDirParam, zDirParam);
        this.factorAnalysisSeed = factorAnalysisSeed;
    }

    public Perspective3DNode(double xParam, double yParam, double zParam,
                             double xDirParam, double yDirParam, double zDirParam) {
        xCoord = xParam;
        yCoord = yParam;
        zCoord = zParam;
        xDirCoord = xDirParam;
        yDirCoord = yDirParam;
        zDirCoord = zDirParam;
    }

    public void setParamsByIndex(int x, int y, int z, int xDir, int yDir, int zDir) {
        xCoord = factorAnalysisSeed.vector[x];
        yCoord = factorAnalysisSeed.vector[y];
        zCoord = factorAnalysisSeed.vector[z];
        xDirCoord = factorAnalysisSeed.vector.length > xDir ? factorAnalysisSeed.vector[xDir]
            : factorAnalysisSeed.vector[factorAnalysisSeed.vector.length - 1];
        yDirCoord = factorAnalysisSeed.vector.length > yDir ? factorAnalysisSeed.vector[yDir]
            : factorAnalysisSeed.vector[factorAnalysisSeed.vector.length - 1];

        zDirCoord = factorAnalysisSeed.vector.length > zDir ? factorAnalysisSeed.vector[zDir]
            : factorAnalysisSeed.vector[factorAnalysisSeed.vector.length - 1];
    }

    public void shiftBy(double xShift, double yShift, double zShift) {
        xCoord -= xShift;
        yCoord -= yShift;
        zCoord -= zShift;
        xDirCoord -= xShift;
        yDirCoord -= yShift;
        zDirCoord -= zShift;
    }

    public void scaleBy(double scale) {
        xCoord *= scale;
        yCoord *= scale;
        zCoord *= scale;
        xDirCoord *= scale;
        yDirCoord *= scale;
        zDirCoord *= scale;
    }

    public static Color getPNodeColor(COLOR_MODE colorMode, COLOR_MAP colorMap, HyperspaceSeed seed
        , double minX, double minY, double minZ, double domainRange) {
        switch (colorMode) {
            case COLOR_BY_LAYER -> {
                if (null == seed.layer)
                    return Color.ALICEBLUE;
                else
                    return FeatureLayer.getColorByIndex(seed.layer);
            }
            case COLOR_BY_GRADIENT -> {
                return Color.color(
                    Utils.clamp(0, DataUtils.normalize(seed.vector[seed.x], minX, minX + domainRange), 1),
                    Utils.clamp(0, DataUtils.normalize(seed.vector[seed.y], minY, minY + domainRange), 1),
                    Utils.clamp(0, DataUtils.normalize(seed.vector[seed.z], minZ, minZ + domainRange), 1),
                    1.0); //full opacity
            }
            case COLOR_BY_SCORE -> {
                if (null == seed.score)
                    return Color.ALICEBLUE;
                if (null != colorMap) switch (colorMap) {
                    case HSB_WHEEL_SPECTRUM -> {
                        return Color.hsb(
                            DataUtils.normalize(seed.score,
                                ColorMap.domainMin1, ColorMap.domainMax1) * 360.0,
                            1, 1);
                    }
                    case ONE_COLOR_SPECTRUM -> {
                        return ColorMap.getInterpolatedColor(seed.score,
                            ColorMap.domainMin1, ColorMap.domainMax1,
                            ColorMap.singleColorSpectrum);
                    }
                    case TWO_COLOR_SPECTRUM -> {
                        return ColorMap.twoColorInterpolation(
                            ColorMap.twoColorSpectrum1, ColorMap.twoColorSpectrum2,
                            ColorMap.domainMin1, ColorMap.domainMax1,
                            seed.score);
                    }
                    case PRESET_COLOR_PALETTE -> {
                        return ColorMap.currentMap.get(
                            DataUtils.normalize(seed.score,
                                ColorMap.domainMin1, ColorMap.domainMax1)
                        );
                    }
                    default -> {
                        return Color.ALICEBLUE;
                    }
                }
                return Color.ALICEBLUE;
            }
            case COLOR_BY_PFA -> {
                if (null == seed.pfa)
                    return Color.ALICEBLUE;

                if (null != colorMap) switch (colorMap) {
                    case HSB_WHEEL_SPECTRUM -> {
                        return Color.hsb(
                            DataUtils.normalize(seed.pfa,
                                ColorMap.domainMin2, ColorMap.domainMax2) * 360.0,
                            1, 1);
                    }
                    case ONE_COLOR_SPECTRUM -> {
                        return ColorMap.getInterpolatedColor(seed.pfa,
                            ColorMap.domainMin2, ColorMap.domainMax2,
                            ColorMap.singleColorSpectrum);
                    }
                    case TWO_COLOR_SPECTRUM -> {
                        return ColorMap.twoColorInterpolation(
                            ColorMap.twoColorSpectrum1, ColorMap.twoColorSpectrum2,
                            ColorMap.domainMin2, ColorMap.domainMax2,
                            seed.pfa);
                    }
                    case PRESET_COLOR_PALETTE -> {
                        return ColorMap.currentMap.get(
                            DataUtils.normalize(seed.pfa,
                                ColorMap.domainMin2, ColorMap.domainMax2)
                        );
                    }
                    default -> {
                        return Color.ALICEBLUE;
                    }
                }
                return Color.ALICEBLUE;
            }
            case COLOR_BY_LABEL -> {
                if (null == seed.label || seed.label.isBlank())
                    return Color.ALICEBLUE;
                else
                    return FactorLabel.getColorByLabel(seed.label);
            }
        }
        return Color.ALICEBLUE;
    }

    //We will need a comparator for binary searches
    public static Comparator<Perspective3DNode> PerspectiveXComparator = (Perspective3DNode p1, Perspective3DNode p2) -> {
        if (p1.xCoord < p2.xCoord) return -1;
        else if (p1.xCoord > p2.xCoord) return 1;
        else return 0;
    };
    //We will need a comparator for Y ordered binary searches
    public static Comparator<Perspective3DNode> PerspectiveYComparator = (Perspective3DNode p1, Perspective3DNode p2) -> {
        if (p1.yCoord < p2.yCoord) return -1;
        else if (p1.yCoord > p2.yCoord) return 1;
        else return 0;
    };
    //We will need a comparator for Z ordered binary searches
    public static Comparator<Perspective3DNode> PerspectiveZComparator = (Perspective3DNode p1, Perspective3DNode p2) -> {
        if (p1.zCoord < p2.zCoord) return -1;
        else if (p1.zCoord > p2.zCoord) return 1;
        else return 0;
    };
}
