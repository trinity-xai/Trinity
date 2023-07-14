package edu.jhuapl.trinity.data.cislunar;

/*-
 * #%L
 * trinity
 * %%
 * Copyright (C) 2021 - 2023 The Johns Hopkins University Applied Physics Laboratory LLC
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import java.util.function.Function;

/**
 * @author Sean Phillips
 */

public class McclodSplitDataTsv {

    //This data is split by the zeta values:
    //0-0.5: It is represented in the Earth-Moon frame
    //0.5-1: It is represented in the Sun-Earth frame
    //
    //The data format for the both frames is equal.
    //Data for BLT files:
    //
    //outputs:
    //Column 1: right ascension in deg
    //Column 2: declination in deg
    //Column 3: right ascension rate in deg/min
    //Column 4: declination rate in deg/min
    //
    //Inputs:
    //Column 5: inclination in deg
    //Column 6: RAAN (right ascension of the ascending node) in deg
    //Column 7: kappa (represents transfer along a family) normalized(0 to 1)
    //Column 8: zeta (represents time along the transfer, normalized by the TOF)

    //Example file content
    //113.351072729091	-4.12994618421393	0.0223854400083046	-0.474764725354219	0.819444444444444	0	0.000336269287801838	0.111111111111111
    //112.176604842365	-6.78685213552351	-0.259212142811593	-0.23658721893974	0.819444444444444	0	0.00333701947534873	0.111111111111111
    //109.26930704041	-8.43842817612934	-0.369972114067144	-0.143576112063013	0.819444444444444	0	0.00671286343633897	0.111111111111111
    //105.252329716951	-9.61487504947684	-0.425952592890388	-0.0968517181234978	0.819444444444444	0	0.0104638011707726	0.111111111111111

    private double rightAscensionDeg;
    private double declinationDeg;
    private double rightAscensionRateDegMin;
    private double declinationRateDegMin;

    private double inclinationDeg;
    private double raanDeg;
    private double kappa;
    private double zeta;

    public static Function<String, McclodSplitDataTsv> tsvToMcclodSplitDataTsv = s -> {
        McclodSplitDataTsv mcclodSplitDataTsv = new McclodSplitDataTsv();
        String[] tokens = s.split("\t");
        int tokenIndex = 0;
        try {
            mcclodSplitDataTsv.setRightAscensionDeg(Double.parseDouble(tokens[tokenIndex++]));
            mcclodSplitDataTsv.setDeclinationDeg(Double.parseDouble(tokens[tokenIndex++]));
            mcclodSplitDataTsv.setRightAscensionRateDegMin(Double.parseDouble(tokens[tokenIndex++]));
            mcclodSplitDataTsv.setDeclinationRateDegMin(Double.parseDouble(tokens[tokenIndex++]));

            mcclodSplitDataTsv.setInclinationDeg(Double.parseDouble(tokens[tokenIndex++]));
            mcclodSplitDataTsv.setRaanDeg(Double.parseDouble(tokens[tokenIndex++]));
            mcclodSplitDataTsv.setKappa(Double.parseDouble(tokens[tokenIndex++]));
            mcclodSplitDataTsv.setZeta(Double.parseDouble(tokens[tokenIndex++]));

        } catch (NumberFormatException ex) {
            ex.printStackTrace();
        }
        return mcclodSplitDataTsv;
    };

    public McclodSplitDataTsv() {

    }

    /**
     * @return the rightAscensionDeg
     */
    public double getRightAscensionDeg() {
        return rightAscensionDeg;
    }

    /**
     * @param rightAscensionDeg the rightAscensionDeg to set
     */
    public void setRightAscensionDeg(double rightAscensionDeg) {
        this.rightAscensionDeg = rightAscensionDeg;
    }

    /**
     * @return the declinationDeg
     */
    public double getDeclinationDeg() {
        return declinationDeg;
    }

    /**
     * @param declinationDeg the declinationDeg to set
     */
    public void setDeclinationDeg(double declinationDeg) {
        this.declinationDeg = declinationDeg;
    }

    /**
     * @return the rightAscensionRateDegMin
     */
    public double getRightAscensionRateDegMin() {
        return rightAscensionRateDegMin;
    }

    /**
     * @param rightAscensionRateDegMin the rightAscensionRateDegMin to set
     */
    public void setRightAscensionRateDegMin(double rightAscensionRateDegMin) {
        this.rightAscensionRateDegMin = rightAscensionRateDegMin;
    }

    /**
     * @return the declinationRateDegMin
     */
    public double getDeclinationRateDegMin() {
        return declinationRateDegMin;
    }

    /**
     * @param declinationRateDegMin the declinationRateDegMin to set
     */
    public void setDeclinationRateDegMin(double declinationRateDegMin) {
        this.declinationRateDegMin = declinationRateDegMin;
    }

    /**
     * @return the inclinationDeg
     */
    public double getInclinationDeg() {
        return inclinationDeg;
    }

    /**
     * @param inclinationDeg the inclinationDeg to set
     */
    public void setInclinationDeg(double inclinationDeg) {
        this.inclinationDeg = inclinationDeg;
    }

    /**
     * @return the raanDeg
     */
    public double getRaanDeg() {
        return raanDeg;
    }

    /**
     * @param raanDeg the raanDeg to set
     */
    public void setRaanDeg(double raanDeg) {
        this.raanDeg = raanDeg;
    }

    /**
     * @return the kappa
     */
    public double getKappa() {
        return kappa;
    }

    /**
     * @param kappa the kappa to set
     */
    public void setKappa(double kappa) {
        this.kappa = kappa;
    }

    /**
     * @return the zeta
     */
    public double getZeta() {
        return zeta;
    }

    /**
     * @param zeta the zeta to set
     */
    public void setZeta(double zeta) {
        this.zeta = zeta;
    }

}
