package edu.jhuapl.trinity.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;

/**
 * @author Sean Phillips
 */
public class SaturnShot {
    private static final Logger LOG = LoggerFactory.getLogger(SaturnShot.class);
    //Example Header and single csv line
    //VarName1,Time_ms,Shutter,X_mm,Y_mm,Z_mm,Power,PD_0,PD_1,PD_2,PD_3
    //20808943,115605.238888889,1,140.648198254215,205.063043298452,3.2,6.248054042,0.000562797779999902,0.0171936016,-0.00541654708,-0.00408584207999962
    
    private long varName1;
    private double timeMs;
    private boolean shutter;
    private double x_mm;
    private double y_mm;
    private double z_mm;
    private double power;
    private double pd_0;
    private double pd_1;
    private double pd_2;
    private double pd_3;
    
    public SaturnShot() { }
    
    public static Function<String, SaturnShot> csvToSaturnShot = s -> {
        SaturnShot shot = new SaturnShot();
        String[] tokens = s.split(",");
        int tokenIndex = 0;
//        try {
            shot.setVarName1(Long.parseLong(tokens[tokenIndex++]));
            shot.setTimeMs(Double.parseDouble(tokens[tokenIndex++]));
            shot.setShutter(Integer.parseInt(tokens[tokenIndex++])!= 0);
            shot.setX_mm(Double.parseDouble(tokens[tokenIndex++]));
            shot.setY_mm(Double.parseDouble(tokens[tokenIndex++]));
            shot.setZ_mm(Double.parseDouble(tokens[tokenIndex++]));
            shot.setPower(Double.parseDouble(tokens[tokenIndex++]));
            shot.setPd_0(Double.parseDouble(tokens[tokenIndex++]));
            shot.setPd_1(Double.parseDouble(tokens[tokenIndex++]));
            shot.setPd_2(Double.parseDouble(tokens[tokenIndex++]));
            shot.setPd_3(Double.parseDouble(tokens[tokenIndex]));

//        } catch (NumberFormatException ex) {
//            LOG.error("Exception", ex);
//        }
        return shot;
    };
    /**
     * @return the varName1
     */
    public long getVarName1() {
        return varName1;
    }

    /**
     * @param varName1 the varName1 to set
     */
    public void setVarName1(long varName1) {
        this.varName1 = varName1;
    }

    /**
     * @return the timeMs
     */
    public double getTimeMs() {
        return timeMs;
    }

    /**
     * @param timeMs the timeMs to set
     */
    public void setTimeMs(double timeMs) {
        this.timeMs = timeMs;
    }

    /**
     * @return the shutter
     */
    public boolean isShutter() {
        return shutter;
    }

    /**
     * @param shutter the shutter to set
     */
    public void setShutter(boolean shutter) {
        this.shutter = shutter;
    }

    /**
     * @return the x_mm
     */
    public double getX_mm() {
        return x_mm;
    }

    /**
     * @param x_mm the x_mm to set
     */
    public void setX_mm(double x_mm) {
        this.x_mm = x_mm;
    }

    /**
     * @return the y_mm
     */
    public double getY_mm() {
        return y_mm;
    }

    /**
     * @param y_mm the y_mm to set
     */
    public void setY_mm(double y_mm) {
        this.y_mm = y_mm;
    }

    /**
     * @return the z_mm
     */
    public double getZ_mm() {
        return z_mm;
    }

    /**
     * @param z_mm the z_mm to set
     */
    public void setZ_mm(double z_mm) {
        this.z_mm = z_mm;
    }

    /**
     * @return the power
     */
    public double getPower() {
        return power;
    }

    /**
     * @param power the power to set
     */
    public void setPower(double power) {
        this.power = power;
    }

    /**
     * @return the pd_0
     */
    public double getPd_0() {
        return pd_0;
    }

    /**
     * @param pd_0 the pd_0 to set
     */
    public void setPd_0(double pd_0) {
        this.pd_0 = pd_0;
    }

    /**
     * @return the pd_1
     */
    public double getPd_1() {
        return pd_1;
    }

    /**
     * @param pd_1 the pd_1 to set
     */
    public void setPd_1(double pd_1) {
        this.pd_1 = pd_1;
    }

    /**
     * @return the pd_2
     */
    public double getPd_2() {
        return pd_2;
    }

    /**
     * @param pd_2 the pd_2 to set
     */
    public void setPd_2(double pd_2) {
        this.pd_2 = pd_2;
    }

    /**
     * @return the pd_3
     */
    public double getPd_3() {
        return pd_3;
    }

    /**
     * @param pd_3 the pd_3 to set
     */
    public void setPd_3(double pd_3) {
        this.pd_3 = pd_3;
    }
}
