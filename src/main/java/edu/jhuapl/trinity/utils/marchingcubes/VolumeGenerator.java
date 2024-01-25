package edu.jhuapl.trinity.utils.marchingcubes;

/*-
 * #%L
 * trinity-2024.01.08
 * %%
 * Copyright (C) 2021 - 2024 The Johns Hopkins University Applied Physics Laboratory LLC
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

/**
 * Created by Primoz on 11. 07. 2016.
 */
public class VolumeGenerator {
    public static char[] generateScalarFieldChar(int []size) {
        final char[] scalarField = new char[size[0] * size[1] * size[2]];
        float axisMin = -10;
        float axisMax = 10;
        float axisRange = axisMax - axisMin;

        for (int k = 0; k < size[0]; k++) {
            for (int j = 0; j < size[1]; j++) {
                for (int i = 0; i < size[2]; i++) {
                    // actual values
                    char x = (char) (axisMin + axisRange * i / (size[0] - 1));
                    char y = (char) (axisMin + axisRange * j / (size[1] - 1));
                    char z = (char) (axisMin + axisRange * k / (size[2] - 1));
                    scalarField[k + size[1] * (j + size[2] * i)] = (char) (x * x + y * y - z * z - 25);
                }
            }
        }

        return scalarField;
    }

    public static short[] generateScalarFieldShort(int []size) {
        final short[] scalarField = new short[size[0] * size[1] * size[2]];
        float axisMin = -10;
        float axisMax = 10;
        float axisRange = axisMax - axisMin;

        for (int k = 0; k < size[0]; k++) {
            for (int j = 0; j < size[1]; j++) {
                for (int i = 0; i < size[2]; i++) {
                    // actual values
                    short x = (short) (axisMin + axisRange * i / (size[0] - 1));
                    short y = (short) (axisMin + axisRange * j / (size[1] - 1));
                    short z = (short) (axisMin + axisRange * k / (size[2] - 1));
                    scalarField[k + size[1] * (j + size[2] * i)] = (short) (x * x + y * y - z * z - 25);
                }
            }
        }

        return scalarField;
    }

    public static int[] generateScalarFieldInt(int []size) {
        final int[] scalarField = new int[size[0] * size[1] * size[2]];
        float axisMin = -10;
        float axisMax = 10;
        float axisRange = axisMax - axisMin;

        for (int k = 0; k < size[0]; k++) {
            for (int j = 0; j < size[1]; j++) {
                for (int i = 0; i < size[2]; i++) {
                    // actual values
                    int x = (int) (axisMin + axisRange * i / (size[0] - 1));
                    int y = (int) (axisMin + axisRange * j / (size[1] - 1));
                    int z = (int) (axisMin + axisRange * k / (size[2] - 1));
                    scalarField[k + size[1] * (j + size[2] * i)] = (int) (x * x + y * y - z * z - 25);
                }
            }
        }

        return scalarField;
    }

    public static float[] generateScalarFieldFloat(int []size) {
        final float[] scalarField = new float[size[0] * size[1] * size[2]];
        float axisMin = -10;
        float axisMax = 10;
        float axisRange = axisMax - axisMin;

        for (int k = 0; k < size[0]; k++) {
            for (int j = 0; j < size[1]; j++) {
                for (int i = 0; i < size[2]; i++) {
                    // actual values
                    float x = axisMin + axisRange * i / (size[0] - 1);
                    float y = axisMin + axisRange * j / (size[1] - 1);
                    float z = axisMin + axisRange * k / (size[2] - 1);
                    scalarField[k + size[1] * (j + size[2] * i)] = x * x + y * y - z * z - 25;
                }
            }
        }

        return scalarField;
    }

    public static double[] generateScalarFieldDouble(int []size) {
        final double[] scalarField = new double[size[0] * size[1] * size[2]];
        double axisMin = -10;
        double axisMax = 10;
        double axisRange = axisMax - axisMin;

        for (int k = 0; k < size[0]; k++) {
            for (int j = 0; j < size[1]; j++) {
                for (int i = 0; i < size[2]; i++) {
                    // actual values
                    double x = axisMin + axisRange * i / (size[0] - 1);
                    double y = axisMin + axisRange * j / (size[1] - 1);
                    double z = axisMin + axisRange * k / (size[2] - 1);
                    scalarField[k + size[1] * (j + size[2] * i)] = (x * x + y * y - z * z - 25);
                }
            }
        }

        return scalarField;
    }
}
