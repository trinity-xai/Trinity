/*******************************************************************************
 *    Original Copyright 2015, 2016 Taylor G Smith
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *******************************************************************************/
package com.clust4j.utils;

import com.clust4j.GlobalState;
import com.clust4j.TestSuite;
import com.clust4j.algo.NearestNeighbors;
import com.clust4j.algo.NearestNeighborsParameters;
import com.clust4j.data.DataSet;
import com.clust4j.data.ExampleDataSets;
import com.clust4j.except.NonUniformMatrixException;
import com.clust4j.utils.MatUtils.Axis;
import com.clust4j.utils.MatUtils.MatSeries;
import com.clust4j.utils.Series.Inequality;
import org.apache.commons.math3.exception.DimensionMismatchException;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

public class MatTests {
    final static DataSet IRIS = ExampleDataSets.loadIris();

    @Test
    public void test() {
        final double[][] data = new double[][]{
            new double[]{0.000, 0.000, 0.000},
            new double[]{1.500, 1.500, 1.500},
            new double[]{3.000, 3.000, 3.000}
        };

        assertTrue(VecUtils.equalsExactly(data[1], MatUtils.meanRecord(data)));
        assertTrue(VecUtils.equalsExactly(data[1], MatUtils.medianRecord(data)));
    }

    @Test
    public void testTinyEps() {
        assertTrue(GlobalState.Mathematics.TINY > 0);
        assertTrue(GlobalState.Mathematics.EPS > 0);
        assertTrue(GlobalState.Mathematics.TINY * 100 > 0);
    }

    @Test
    public void test2() {
        final double[][] data = new double[][]{
            new double[]{0.000, 0.000, 0.000},
            new double[]{1.500, 1.500, 1.500},
            new double[]{3.000, 3.000, 3.000}
        };

        final double[][] data2 = new double[][]{
            new double[]{0.000, 0.000, 0.000},
            new double[]{1.500, 1.500, 1.500},
            new double[]{3.000, 3.000, 3.000}
        };

        assertTrue(MatUtils.equalsExactly(data, data2));
    }

    @Test
    public void test3() {
        final double[][] data = new double[][]{
            new double[]{0.000, 0.000, 0.000},
            new double[]{1.500, 1.500, 1.500},
            new double[]{3.000, 3.000, 3.000}
        };

        final double[][] data2 = new double[][]{
            new double[]{1.000, 0.000, 0.000},
            new double[]{1.500, 1.500, 1.500},
            new double[]{3.000, 3.000, 3.000}
        };

        final double[][] data3 = new double[][]{
            new double[]{1.000, 0.000, 0.000},
            new double[]{3.000, 3.000, 3.000},
            new double[]{6.000, 6.000, 6.000}
        };

        assertTrue(MatUtils.equalsExactly(data3, MatUtils.add(data, data2)));
    }

    @Test
    public void testArgs() {
        final double[][] data = new double[][]{
            new double[]{0.000, 0.000, 0.000},
            new double[]{1.500, 1.500, 1.500},
            new double[]{3.000, 3.000, 3.000}
        };

        assertTrue(VecUtils.equalsExactly(new int[]{2, 2, 2}, MatUtils.argMax(data, MatUtils.Axis.COL)));
        assertTrue(VecUtils.equalsExactly(new int[]{0, 0, 0}, MatUtils.argMin(data, MatUtils.Axis.COL)));
        assertTrue(VecUtils.equalsExactly(new int[]{0, 0, 0}, MatUtils.argMax(data, MatUtils.Axis.ROW)));
        assertTrue(VecUtils.equalsExactly(new int[]{0, 0, 0}, MatUtils.argMin(data, MatUtils.Axis.ROW)));
    }

    @Test
    public void testMinMaxes() {
        final double[][] data = new double[][]{
            new double[]{0.000, 0.000, 0.000},
            new double[]{1.500, 1.500, 1.500},
            new double[]{3.000, 3.000, 3.000}
        };

        assertTrue(VecUtils.equalsExactly(new double[]{3, 3, 3}, MatUtils.max(data, MatUtils.Axis.COL)));
        assertTrue(VecUtils.equalsExactly(new double[]{0, 0, 0}, MatUtils.min(data, MatUtils.Axis.COL)));
        assertTrue(VecUtils.equalsExactly(new double[]{0, 1.5, 3}, MatUtils.max(data, MatUtils.Axis.ROW)));
        assertTrue(VecUtils.equalsExactly(new double[]{0, 1.5, 3}, MatUtils.min(data, MatUtils.Axis.ROW)));
    }

    @Test
    public void testFromVector() {
        final double[] a = new double[]{0, 1, 3};

        final double[][] data = new double[][]{
            new double[]{0.000, 0.000, 0.000},
            new double[]{1.000, 1.000, 1.000},
            new double[]{3.000, 3.000, 3.000}
        };

        final double[][] data2 = new double[][]{
            new double[]{0.000, 1.000, 3.000},
            new double[]{0.000, 1.000, 3.000},
            new double[]{0.000, 1.000, 3.000}
        };

        assertTrue(MatUtils.equalsExactly(MatUtils.fromVector(a, 3, Axis.ROW), data));
        assertTrue(MatUtils.equalsExactly(MatUtils.fromVector(a, 3, Axis.COL), data2));
    }

    @Test
    public void testRowColSumsMeans() {
        final double[] a = new double[]{4, 4, 4};
        final double[] b = new double[]{0, 3, 9};
        final double[] c = new double[]{4.0 / 3.0, 4.0 / 3.0, 4.0 / 3.0};
        final double[] d = new double[]{0.0, 1.0, 3.0};

        final double[][] data = new double[][]{
            new double[]{0.000, 0.000, 0.000},
            new double[]{1.000, 1.000, 1.000},
            new double[]{3.000, 3.000, 3.000}
        };

        assertTrue(VecUtils.equalsExactly(a, MatUtils.colSums(data)));
        assertTrue(VecUtils.equalsExactly(b, MatUtils.rowSums(data)));
        assertTrue(VecUtils.equalsExactly(c, MatUtils.colMeans(data)));
        assertTrue(VecUtils.equalsExactly(d, MatUtils.rowMeans(data)));
    }

    @Test
    public void testDiag() {
        final double[] a = new double[]{0, 1, 3};

        final double[][] data = new double[][]{
            new double[]{0.000, 0.000, 0.000},
            new double[]{1.000, 1.000, 1.000},
            new double[]{3.000, 3.000, 3.000}
        };

        assertTrue(VecUtils.equalsExactly(MatUtils.diagFromSquare(data), a));
    }

    @Test
    public void testInPlace() {
        final double[] a = new double[]{0, 1, 3};

        final double[][] data = new double[][]{
            new double[]{0.000, 0.000, 0.000},
            new double[]{1.000, 1.000, 1.000},
            new double[]{3.000, 3.000, 3.000}
        };

        MatUtils.setRowInPlace(data, 0, a);
        assertTrue(VecUtils.equalsExactly(a, data[0]));

        MatUtils.setColumnInPlace(data, 0, a);
        assertTrue(VecUtils.equalsExactly(MatUtils.getColumn(data, 0), a));
    }

    @Test
    public void testPosInf() {
        final double[][] data = new double[][]{
            new double[]{0.000, 0.000, 0.000},
            new double[]{1.000, Double.POSITIVE_INFINITY, 1.000},
            new double[]{3.000, 3.000, 3.000}
        };

        assertTrue(MatUtils.containsInf(data));
    }

    @Test
    public void testNegInf() {
        final double[][] data = new double[][]{
            new double[]{0.000, 0.000, 0.000},
            new double[]{1.000, Double.NEGATIVE_INFINITY, 1.000},
            new double[]{3.000, 3.000, 3.000}
        };

        assertTrue(MatUtils.containsInf(data));
    }

    @Test
    public void testFlatten() {
        final double[] a = new double[]{0, 0, 0, 1, 1, 1, 3, 3, 3};

        final double[][] data = new double[][]{
            new double[]{0.000, 0.000, 0.000},
            new double[]{1.000, 1.000, 1.000},
            new double[]{3.000, 3.000, 3.000}
        };

        assertTrue(VecUtils.equalsExactly(MatUtils.flatten(data), a));
    }

    @Test
    public void testCubing() {
        final double[][] data = new double[][]{
            new double[]{0.000, 0.000, 0.000},
            new double[]{1.000, 1.000, 1.000},
            new double[]{3.000, 3.000, 3.000}
        };

        int[] idcs = new int[]{0, 1};
        double[][] cube = MatUtils.getRows(MatUtils.getColumns(data, idcs), idcs);
        assertTrue(cube[0][0] == data[0][0]);
        assertTrue(cube[0][1] == data[0][1]);
        assertTrue(cube[1][0] == data[1][0]);
        assertTrue(cube[1][1] == data[1][1]);

        idcs = new int[]{1, 2};
        cube = MatUtils.getRows(MatUtils.getColumns(data, idcs), idcs);
        assertTrue(cube[0][0] == data[1][1]);
        assertTrue(cube[0][1] == data[1][2]);
        assertTrue(cube[1][0] == data[2][1]);
        assertTrue(cube[1][1] == data[2][2]);
    }

    @Test
    public void testMatCheck() {
        assertThrows(IllegalArgumentException.class, () -> {
            MatUtils.checkDims(new double[5][]);
        });
    }

    public void testMatVecScalarOperations() {
        final double[][] data = new double[][]{
            new double[]{0.000, 0.000, 0.000},
            new double[]{1.000, 1.000, 1.000},
            new double[]{3.000, 3.000, 3.000}
        };

        final double[] operator = new double[]{1, 2, 3};


        // Addition
        double[][] addedRowWise = MatUtils.scalarAdd(data, operator, Axis.ROW);
        assertTrue(MatUtils.equalsExactly(addedRowWise, new double[][]{
            new double[]{1.000, 1.000, 1.000},
            new double[]{3.000, 3.000, 3.000},
            new double[]{6.000, 6.000, 6.000}
        }));

        double[][] addedColWise = MatUtils.scalarAdd(data, operator, Axis.COL);
        assertTrue(MatUtils.equalsExactly(addedColWise, new double[][]{
            new double[]{1.000, 2.000, 3.000},
            new double[]{2.000, 3.000, 4.000},
            new double[]{4.000, 5.000, 6.000}
        }));


        // Subtraction
        double[][] subRowWise = MatUtils.scalarSubtract(data, operator, Axis.ROW);
        assertTrue(MatUtils.equalsExactly(subRowWise, new double[][]{
            new double[]{-1.000, -1.000, -1.000},
            new double[]{-1.000, -1.000, -1.000},
            new double[]{0.000, 0.000, 0.000}
        }));

        double[][] subColWise = MatUtils.scalarSubtract(data, operator, Axis.COL);
        assertTrue(MatUtils.equalsExactly(subColWise, new double[][]{
            new double[]{-1.000, -2.000, -3.000},
            new double[]{0.000, -1.000, -2.000},
            new double[]{2.000, 1.000, 0.000}
        }));


        double[][] YM = MatUtils.fromVector(operator, 3, Axis.ROW);
        assertTrue(MatUtils.equalsExactly(subRowWise, MatUtils.subtract(data, YM)));
    }

    @Test
    public void testKNearestStatic() {
        final double[][] mat = new double[][]{
            new double[]{-1.000, -1.000, -1.000},
            new double[]{10.000, 10.000, 10.000},
            new double[]{90.000, 90.000, 90.000}
        };

        final double[] record = new double[]{0, 0, 0};
        NearestNeighbors nn = new NearestNeighborsParameters(1).fitNewModel(new Array2DRowRealMatrix(mat, false));
        assertTrue(nn.getNeighbors(new Array2DRowRealMatrix(new double[][]{record},
            false)).getIndices()[0][0] == 0);
    }

    @Test
    public void testTrans() {
        final double[][] mat = new double[][]{
            new double[]{-1.000, -1.000, -1.000},
            new double[]{10.000, 10.000, 10.000},
            new double[]{90.000, 90.000, 90.000}
        };

        Array2DRowRealMatrix matrix = new Array2DRowRealMatrix(mat);
        assertTrue(MatUtils.equalsExactly(MatUtils.transpose(mat), matrix.transpose().getData()));
    }

    @Test
    public void testWhere() {
        final double[][] a = new double[][]{
            new double[]{6, 0},
            new double[]{7, 8}
        };

        final MatSeries ser = new MatSeries(a, Inequality.GREATER_THAN, 5);
        final double[][] b = new double[][]{
            new double[]{1, 2},
            new double[]{3, 4}
        };

        final double[][] c = new double[][]{
            new double[]{9, 8},
            new double[]{7, 6}
        };

        final double[][] d = new double[][]{
            new double[]{1, 8},
            new double[]{3, 4}
        };

        assertTrue(MatUtils.equalsExactly(d, MatUtils.where(ser, b, c)));

        // Test if one of the matrices is empty
        boolean pass = false;
        try {
            pass = false;
            MatUtils.where(ser, new double[][]{}, c);
        } catch (IllegalArgumentException e) {
            pass = true;
        } finally {
            if (!pass)
                fail();
        }

        try {
            pass = false;
            MatUtils.where(ser, b, new double[][]{});
        } catch (IllegalArgumentException e) {
            pass = true;
        } finally {
            if (!pass)
                fail();
        }

        // Test for non-uniformity
        try {
            pass = false;
            MatUtils.where(ser,
                new double[][]{
                    new double[]{1, 2, 3},
                    new double[]{1}
                }, c);
        } catch (NonUniformMatrixException e) {
            pass = true;
        } finally {
            if (!pass)
                fail();
        }

        try {
            pass = false;
            MatUtils.where(ser, b,
                new double[][]{
                    new double[]{1, 2, 3},
                    new double[]{1}
                });
        } catch (NonUniformMatrixException e) {
            pass = true;
        } finally {
            if (!pass)
                fail();
        }

        // Test for dim mismatches
        try { // where c is the odd man out
            pass = false;
            MatUtils.where(ser, b,
                new double[][]{
                    new double[]{1, 2, 3},
                    new double[]{1, 2, 3}
                });
        } catch (DimensionMismatchException e) {
            pass = true;
        } finally {
            if (!pass)
                fail();
        }

        try { // where b is the odd man out
            pass = false;
            MatUtils.where(ser,
                new double[][]{
                    new double[]{1, 2, 3},
                    new double[]{1, 2, 3}
                }, c);
        } catch (DimensionMismatchException e) {
            pass = true;
        } finally {
            if (!pass)
                fail();
        }

        try { // where ser is the odd man out by col dim
            pass = false;
            MatUtils.where(ser,
                new double[][]{
                    new double[]{1, 2, 3},
                    new double[]{1, 2, 3}
                },
                new double[][]{
                    new double[]{1, 2, 3},
                    new double[]{1, 2, 3}
                });
        } catch (DimensionMismatchException e) {
            pass = true;
        } finally {
            if (!pass)
                fail();
        }

        try { // where ser is the odd man out by row dim
            pass = false;
            MatUtils.where(ser,
                new double[][]{
                    new double[]{1, 2},
                    new double[]{1, 2},
                    new double[]{1, 2}
                },
                new double[][]{
                    new double[]{1, 2},
                    new double[]{1, 2},
                    new double[]{1, 2}
                });
        } catch (DimensionMismatchException e) {
            pass = true;
        } finally {
            if (!pass)
                fail();
        }

        //vector 'where' tests
        double[] x = new double[]{1, 2, 3};
        double[] y = new double[]{3, 2, 1};
        MatSeries s = new MatSeries(new double[][]{
            new double[]{-1, 0, 1},
            new double[]{0, -1, 2},
        }, Inequality.GREATER_THAN_OR_EQUAL_TO, 0);

        // {false, true , true }
        // {true , false, true }
        double[][] z = MatUtils.where(s, x, y);
        assertTrue(MatUtils.equalsExactly(z,
            new double[][]{
                new double[]{3, 2, 3},
                new double[]{1, 2, 3}
            }));

        double[][] xm = new double[][]{
            new double[]{1, 2, 3},
            new double[]{4, 5, 6}
        };
        z = MatUtils.where(s, xm, y);
        assertTrue(MatUtils.equalsExactly(z,
            new double[][]{
                new double[]{3, 2, 3},
                new double[]{4, 2, 6}
            }));

        double[][] ym = new double[][]{
            new double[]{9, 8, 5},
            new double[]{-1, -5, 4}
        };
        z = MatUtils.where(s, x, ym);
        assertTrue(MatUtils.equalsExactly(z,
            new double[][]{
                new double[]{9, 2, 3},
                new double[]{1, -5, 3}
            }));
    }

    @Test
    public void testMatSeries() {
        // Test constructor A with matrix and static value
        double[][] a = new double[][]{
            new double[]{4, 1, 2},
            new double[]{2, 6, 0},
            new double[]{9, 2, 0}
        };

        MatSeries series = new MatSeries(a, Inequality.EQUAL_TO, 0);
        assertTrue(MatUtils.equalsExactly(series.getRef(),
            new boolean[][]{
                new boolean[]{false, false, false},
                new boolean[]{false, false, true},
                new boolean[]{false, false, true}
            }));

        series = new MatSeries(a, Inequality.GREATER_THAN, 0);
        assertTrue(MatUtils.equalsExactly(series.getRef(),
            new boolean[][]{
                new boolean[]{true, true, true},
                new boolean[]{true, true, false},
                new boolean[]{true, true, false}
            }));

        // Ensure are the same
        assertTrue(MatUtils.equalsExactly(series.getRef(), series.get()));

        series = new MatSeries(a, Inequality.LESS_THAN, 0);
        assertTrue(MatUtils.equalsExactly(series.getRef(),
            new boolean[][]{
                new boolean[]{false, false, false},
                new boolean[]{false, false, false},
                new boolean[]{false, false, false}
            }));

        // Test for NUME and IAE
        boolean pass = false;
        try {
            pass = false;
            new MatSeries(
                new double[][]{
                    new double[]{1, 2},
                    new double[]{1}
                }, Inequality.EQUAL_TO, 0);
        } catch (NonUniformMatrixException e) {
            pass = true;
        } finally {
            if (!pass)
                fail();
        }

        try {
            pass = false;
            new MatSeries(
                new double[][]{
                }, Inequality.EQUAL_TO, 0);
        } catch (IllegalArgumentException e) {
            pass = true;
        } finally {
            if (!pass)
                fail();
        }


        // Test constructor B with matrix and vector values
		/*
		a = new double[][]{
			new double[]{4,1,2},
			new double[]{2,6,0},
			new double[]{9,2,0}
		};
		*/

        series = new MatSeries(new double[]{1, 2, 3}, Inequality.EQUAL_TO, a);
        assertTrue(MatUtils.equalsExactly(series.getRef(),
            new boolean[][]{
                new boolean[]{false, false, false},
                new boolean[]{false, false, false},
                new boolean[]{false, true, false}
            }));

        series = new MatSeries(new double[]{1, 2, 3}, Inequality.GREATER_THAN, a);
        assertTrue(MatUtils.equalsExactly(series.getRef(),
            new boolean[][]{
                new boolean[]{false, true, true},
                new boolean[]{false, false, true},
                new boolean[]{false, false, true}
            }));


        // Test for NUME, DME and IAE
        try {
            pass = false;
            new MatSeries(new double[]{0, 0}, Inequality.EQUAL_TO,
                new double[][]{
                    new double[]{1, 2},
                    new double[]{1}
                });
        } catch (NonUniformMatrixException e) {
            pass = true;
        } finally {
            if (!pass)
                fail();
        }

        try {
            pass = false;
            new MatSeries(new double[]{}, Inequality.EQUAL_TO,
                new double[][]{
                    new double[]{1, 2},
                    new double[]{1, 2}
                });
        } catch (DimensionMismatchException d) {
            pass = true;
        } finally {
            if (!pass)
                fail();
        }

        try {
            pass = false;
            new MatSeries(new double[]{1, 2, 3}, Inequality.EQUAL_TO,
                new double[][]{
                    new double[]{1, 2},
                    new double[]{1, 2}
                });
        } catch (DimensionMismatchException d) {
            pass = true;
        } finally {
            if (!pass)
                fail();
        }

        try {
            pass = false;
            new MatSeries(new double[]{0, 0}, Inequality.EQUAL_TO,
                new double[][]{
                });
        } catch (IllegalArgumentException e) {
            pass = true;
        } finally {
            if (!pass)
                fail();
        }
    }

    @Test
    public void testNUMEMatSeries1() {
        assertThrows(NonUniformMatrixException.class, () -> {
            double[][] jagged = new double[][]{
                new double[]{0, 1, 2},
                new double[]{2}
            };

            new MatSeries(jagged, Inequality.EQUAL_TO, 0);
        });
    }

    @Test
    public void testNUMEMatSeries2() {
        assertThrows(NonUniformMatrixException.class, () -> {
            double[][] jagged = new double[][]{
                new double[]{0, 1, 2},
                new double[]{2}
            };

            new MatSeries(new double[]{1, 2, 3}, Inequality.EQUAL_TO, jagged);
        });
    }

    @Test
    public void testDMEMatSeries1() {
        assertThrows(DimensionMismatchException.class, () -> {
            double[][] a = new double[][]{
                new double[]{0, 1, 2},
                new double[]{2, 1, 0}
            };

            new MatSeries(new double[]{1, 2}, Inequality.EQUAL_TO, a);
        });
    }

    @Test
    public void testTransposeVector() {
        final double[] a = new double[]{1, 2, 3};
        final double[][] b = new double[][]{
            new double[]{1},
            new double[]{2},
            new double[]{3}
        };

        assertTrue(MatUtils.equalsExactly(b, MatUtils.transpose(a)));
    }

    @Test
    public void testFromVec() {
        final double[] a = new double[]{1, 2, 3};
        final double[][] b = new double[][]{
            new double[]{1, 2, 3},
            new double[]{1, 2, 3},
            new double[]{1, 2, 3}
        };

        assertTrue(MatUtils.equalsExactly(b, MatUtils.rep(a, 3)));
    }

    @Test
    public void testWhere2() {
        final double[][] a = new double[][]{
            new double[]{0, 1, 1},
            new double[]{1, 0, 1},
            new double[]{0, 0, 1}
        };

        MatSeries ser = new MatSeries(a, Inequality.EQUAL_TO, 1);
        final double[] b = new double[]{2, 3, 4};
        final double[][] c = new double[][]{
            new double[]{1, 2, 3},
            new double[]{4, 5, 6},
            new double[]{7, 8, 9}
        };

        final double[][] d = new double[][]{
            new double[]{1, 3, 4},
            new double[]{2, 5, 4},
            new double[]{7, 8, 4}
        };

        assertTrue(MatUtils.equalsExactly(d, MatUtils.where(ser, b, c)));
    }

    @Test
    public void testReshape() {
        final double[][] a = new double[][]{
            new double[]{1, 2, 3},
            new double[]{4, 5, 6},
            new double[]{7, 8, 9},
            new double[]{10, 11, 12},
            new double[]{13, 14, 15}
        };

        final double[][] b = MatUtils.reshape(a, 3, 5);
        //System.out.println(TestSuite.formatter.format(b));
        assertTrue(b.length == 3);
        assertTrue(b[0].length == 5);
    }

    @Test
    public void testAbsJagged() {
        final double[][] a = new double[][]{
            new double[]{1, -2, 3},
            new double[]{4, 6},
            new double[]{},
            new double[]{10, -11, 12},
            new double[]{13, -14, -15}
        };

        final double[][] b = new double[][]{
            new double[]{1, 2, 3},
            new double[]{4, 6},
            new double[]{},
            new double[]{10, 11, 12},
            new double[]{13, 14, 15}
        };

        assertTrue(MatUtils.equalsExactly(MatUtils.abs(a), b));
    }


    @Test
    public void testAddJagged() {
        assertThrows(NonUniformMatrixException.class, () -> {
            final double[][] a = new double[][]{
                new double[]{1, -2, 3},
                new double[]{4, 6},
                new double[]{},
                new double[]{10, -11, 12},
                new double[]{13, -14, -15}
            };

            final double[][] b = new double[][]{
                new double[]{1, 2, 3},
                new double[]{4, 6},
                new double[]{},
                new double[]{10, 11, 12},
                new double[]{13, 14, 15}
            };

            MatUtils.add(a, b);
        });
    }


    @Test
    public void testAddEmpty() {
        final double[][] a = new double[][]{
            new double[]{},
            new double[]{},
            new double[]{},
            new double[]{},
            new double[]{}
        };

        final double[][] b = new double[][]{
            new double[]{},
            new double[]{},
            new double[]{},
            new double[]{},
            new double[]{}
        };

        assertTrue(MatUtils.equalsExactly(MatUtils.add(a, b), a));
    }

    @Test
    public void testArgMaxMin() {
        final double[][] a = new double[][]{
            new double[]{1, 2, 3},
            new double[]{4, 5, 6},
            new double[]{7, 8, 9},
            new double[]{10, 11, 12},
            new double[]{13, 14, 15}
        };

        final int[] argMax = MatUtils.argMax(a, Axis.ROW);
        assertTrue(VecUtils.equalsExactly(argMax, new int[]{2, 2, 2, 2, 2}));

        final int[] argMaxCol = MatUtils.argMax(a, Axis.COL);
        assertTrue(VecUtils.equalsExactly(argMaxCol, new int[]{4, 4, 4}));

        final int[] argMin = MatUtils.argMin(a, Axis.ROW);
        assertTrue(VecUtils.equalsExactly(argMin, new int[]{0, 0, 0, 0, 0}));

        final int[] argMinCol = MatUtils.argMin(a, Axis.COL);
        assertTrue(VecUtils.equalsExactly(argMinCol, new int[]{0, 0, 0}));
    }

    @Test
    public void testArgMinMaxEmpty() {
        final double[][] a = new double[][]{};
        assertTrue(VecUtils.equalsExactly(new int[]{}, MatUtils.argMax(a, Axis.ROW)));
        assertTrue(VecUtils.equalsExactly(new int[]{}, MatUtils.argMax(a, Axis.COL)));
        assertTrue(VecUtils.equalsExactly(new int[]{}, MatUtils.argMin(a, Axis.ROW)));
        assertTrue(VecUtils.equalsExactly(new int[]{}, MatUtils.argMin(a, Axis.COL)));
    }

    @Test
    public void testArgMaxMinNUME1() {
        assertThrows(NonUniformMatrixException.class, () -> {
            final double[][] a = new double[][]{
                new double[]{1, 2, 3},
                new double[]{4},
                new double[]{7, 9},
                new double[]{10, 11, 12, 12},
                new double[]{}
            };

            MatUtils.argMax(a, Axis.ROW);
        });
    }

    @Test
    public void testArgMaxMinNUME2() {
        assertThrows(NonUniformMatrixException.class, () -> {
            final double[][] a = new double[][]{
                new double[]{1, 2, 3},
                new double[]{4},
                new double[]{7, 9},
                new double[]{10, 11, 12, 12},
                new double[]{}
            };

            MatUtils.argMin(a, Axis.ROW);
        });
    }

    @Test
    public void testColMeanSumNUME1() {
        assertThrows(NonUniformMatrixException.class, () -> {
            final double[][] a = new double[][]{
                new double[]{1, 2, 3},
                new double[]{4},
                new double[]{7, 9},
                new double[]{10, 11, 12, 12},
                new double[]{}
            };

            MatUtils.colMeans(a);
        });
    }

    @Test
    public void testColMeanSumNUME2() {
        assertThrows(NonUniformMatrixException.class, () -> {
            final double[][] a = new double[][]{
                new double[]{1, 2, 3},
                new double[]{4},
                new double[]{7, 9},
                new double[]{10, 11, 12, 12},
                new double[]{}
            };

            MatUtils.colSums(a);
        });
    }

    @Test
    public void testCompleteCases() {
        final double[][] a = new double[][]{new double[]{}};
        assertTrue(MatUtils.equalsExactly(MatUtils.completeCases(a), a));

        final double[][] b = new double[][]{
            new double[]{1, 2, 3},
            new double[]{Double.NaN, 2, 3},
            new double[]{4, 2, 3}
        };

        final Array2DRowRealMatrix mat = new Array2DRowRealMatrix(b);

        final double[][] c = new double[][]{
            new double[]{1, 2, 3},
            new double[]{4, 2, 3}
        };

        assertTrue(MatUtils.containsNaN(b));
        assertTrue(MatUtils.containsNaN(mat));
        assertFalse(MatUtils.containsNaN(c));
        assertTrue(MatUtils.equalsExactly(MatUtils.completeCases(mat), c));
    }

    @Test
    public void testCopyDouble() {
        double[][] a = new double[][]{new double[]{}};
        double[][] b = MatUtils.copy(a);
        assertTrue(MatUtils.equalsExactly(a, b));

        a = new double[][]{
            new double[]{1, 2, 3},
            new double[]{4, 5, 6}
        };

        b = MatUtils.copy(a);
        b[0][0] = 9;

        assertFalse(a[0][0] == b[0][0]);
    }

    @Test
    public void testCopyBoolean() {
        boolean[][] a = new boolean[][]{new boolean[]{}};
        boolean[][] b = MatUtils.copy(a);
        assertTrue(MatUtils.equalsExactly(a, b));

        a = new boolean[][]{
            new boolean[]{true, false, true},
            new boolean[]{false, true, false}
        };

        b = MatUtils.copy(a);
        b[0][0] = false;

        assertFalse(a[0][0] == b[0][0]);
    }

    @Test
    public void testCopyInt() {
        int[][] a = new int[][]{new int[]{}};
        int[][] b = MatUtils.copy(a);
        assertTrue(MatUtils.equalsExactly(a, b));

        a = new int[][]{
            new int[]{0, 1, 0},
            new int[]{1, 0, 1}
        };

        b = MatUtils.copy(a);
        b[0][0] = 1;

        assertFalse(a[0][0] == b[0][0]);
    }

    @Test
    public void testDiagonal() {
        double[][] a = new double[][]{
            new double[]{1, 0, 0},
            new double[]{0, 1, 0},
            new double[]{0, 0, 1}
        };

        assertTrue(VecUtils.equalsExactly(MatUtils.diagFromSquare(a), new double[]{1, 1, 1}));
    }

    @Test
    public void testDiagonalDME() {
        assertThrows(DimensionMismatchException.class, () -> {
            double[][] a = new double[][]{
                new double[]{1, 0, 0},
                new double[]{0, 1, 0}
            };

            MatUtils.diagFromSquare(a);
        });
    }

    @Test
    public void testDiagonalNUME() {
        assertThrows(NonUniformMatrixException.class, () -> {
            double[][] a = new double[][]{
                new double[]{1, 0, 0},
                new double[]{0, 1},
                new double[]{0, 0, 1}
            };

            MatUtils.diagFromSquare(a);
        });
    }

    @Test
    public void testJaggedMultiArrayDims() {
        double[][] a = new double[][]{
            new double[]{1, 2, 4},
            new double[]{2}
        };

        double[][] b = new double[][]{
            new double[]{1, 2, 4},
            new double[]{2}
        };

        MatUtils.checkDims(a, b);
    }

    @Test
    public void testJaggedMultiIntArrayDims() {
        int[][] a = new int[][]{
            new int[]{1, 2, 4},
            new int[]{2}
        };

        int[][] b = new int[][]{
            new int[]{1, 2, 4},
            new int[]{2}
        };

        MatUtils.checkDims(a, b);
    }

    @Test
    public void testJaggedMultiBooleanArrayDims() {
        boolean[][] a = new boolean[][]{
            new boolean[]{true, false, true},
            new boolean[]{true}
        };

        boolean[][] b = new boolean[][]{
            new boolean[]{false, true, true},
            new boolean[]{false}
        };

        MatUtils.checkDims(a, b);
    }

    @Test
    public void testJaggedMultiArrayDims2() {
        assertThrows(DimensionMismatchException.class, () -> {
            double[][] a = new double[][]{
                new double[]{1, 2},
                new double[]{2}
            };

            double[][] b = new double[][]{
                new double[]{1, 2, 4},
                new double[]{2}
            };

            MatUtils.checkDims(a, b);
        });
    }

    @Test
    public void testJaggedMultiIntArrayDims2() {
        assertThrows(DimensionMismatchException.class, () -> {
            int[][] a = new int[][]{
                new int[]{1, 2},
                new int[]{2}
            };

            int[][] b = new int[][]{
                new int[]{1, 2, 4},
                new int[]{2}
            };

            MatUtils.checkDims(a, b);
        });
    }

    @Test
    public void testJaggedMultiBooleanArrayDims2() {
        assertThrows(DimensionMismatchException.class, () -> {
            boolean[][] a = new boolean[][]{
                new boolean[]{true, false},
                new boolean[]{false}
            };

            boolean[][] b = new boolean[][]{
                new boolean[]{false, false, false},
                new boolean[]{true}
            };

            MatUtils.checkDims(a, b);
        });
    }

    @Test
    public void testEmptyMultiArrayDims() {
        assertThrows(IllegalArgumentException.class, () -> {
            double[][] a = new double[][]{};
            double[][] b = new double[][]{};
            MatUtils.checkDims(a, b);
        });
    }

    @Test
    public void testEmptyMultiIntArrayDims() {
        assertThrows(IllegalArgumentException.class, () -> {
            int[][] a = new int[][]{};
            int[][] b = new int[][]{};
            MatUtils.checkDims(a, b);
        });
    }

    @Test
    public void testEmptyMultiBooleanArrayDims() {
        assertThrows(IllegalArgumentException.class, () -> {
            boolean[][] a = new boolean[][]{};
            boolean[][] b = new boolean[][]{};
            MatUtils.checkDims(a, b);
        });
    }

    @Test
    public void testJaggedMultiArrayDims3() {
        assertThrows(DimensionMismatchException.class, () -> {
            double[][] a = new double[][]{
                new double[]{1, 2, 3}
            };

            double[][] b = new double[][]{
                new double[]{1, 2, 4},
                new double[]{2}
            };

            MatUtils.checkDims(a, b);
        });
    }

    @Test
    public void testJaggedMultiIntArrayDims3() {
        assertThrows(DimensionMismatchException.class, () -> {
            int[][] a = new int[][]{
                new int[]{1, 2, 3}
            };

            int[][] b = new int[][]{
                new int[]{1, 2, 4},
                new int[]{2}
            };

            MatUtils.checkDims(a, b);
        });
    }

    @Test
    public void testJaggedMultiBooleanArrayDims3() {
        assertThrows(DimensionMismatchException.class, () -> {
            boolean[][] a = new boolean[][]{
                new boolean[]{false, false, true}
            };

            boolean[][] b = new boolean[][]{
                new boolean[]{true, true, true},
                new boolean[]{false}
            };

            MatUtils.checkDims(a, b);
        });
    }

    @Test
    public void testJaggedMultiArrayDims4() {
        assertThrows(IllegalArgumentException.class, () -> {
            double[][] a = new double[5][];
            double[][] b = new double[5][];

            MatUtils.checkDims(a, b);
        });
    }

    @Test
    public void testJaggedMultiIntArrayDims4() {
        assertThrows(IllegalArgumentException.class, () -> {
            int[][] a = new int[5][];
            int[][] b = new int[5][];

            MatUtils.checkDims(a, b);
        });
    }

    @Test
    public void testJaggedMultiBooleanArrayDims4() {
        assertThrows(IllegalArgumentException.class, () -> {
            boolean[][] a = new boolean[5][];
            boolean[][] b = new boolean[5][];

            MatUtils.checkDims(a, b);
        });
    }

    @Test
    public void testMultiArrayUniformity1() {
        double[][] a = new double[][]{
            new double[]{1, 2, 7},
            new double[]{2, 2, 3}
        };

        double[][] b = new double[][]{
            new double[]{1, 2, 4},
            new double[]{2, 1, 9}
        };

        MatUtils.checkDimsForUniformity(a, b);
    }

    @Test
    public void testMultiArrayIntUniformity1() {
        int[][] a = new int[][]{
            new int[]{1, 2, 7},
            new int[]{2, 2, 3}
        };

        int[][] b = new int[][]{
            new int[]{1, 2, 4},
            new int[]{2, 1, 9}
        };

        MatUtils.checkDimsForUniformity(a, b);
    }

    @Test
    public void testMultiArrayBooleanUniformity1() {
        boolean[][] a = new boolean[][]{
            new boolean[]{true, true, true},
            new boolean[]{true, true, true}
        };

        boolean[][] b = new boolean[][]{
            new boolean[]{true, false, true},
            new boolean[]{false, true, true}
        };

        MatUtils.checkDimsForUniformity(a, b);
    }

    @Test
    public void testMultiArrayUniformity2() {
        assertThrows(DimensionMismatchException.class, () -> {
            double[][] a = new double[][]{
                new double[]{1, 2, 7}
            };

            double[][] b = new double[][]{
                new double[]{1, 2, 4},
                new double[]{2, 1, 9}
            };

            MatUtils.checkDimsForUniformity(a, b);
        });
    }

    @Test
    public void testMultiArrayIntUniformity2() {
        assertThrows(DimensionMismatchException.class, () -> {
            int[][] a = new int[][]{
                new int[]{1, 2, 7}
            };

            int[][] b = new int[][]{
                new int[]{1, 2, 4},
                new int[]{2, 1, 9}
            };

            MatUtils.checkDimsForUniformity(a, b);
        });
    }

    @Test
    public void testMultiArrayBooleanUniformity2() {
        assertThrows(DimensionMismatchException.class, () -> {
            boolean[][] a = new boolean[][]{
                new boolean[]{false, true, false}
            };

            boolean[][] b = new boolean[][]{
                new boolean[]{true, true, true},
                new boolean[]{false, true, true}
            };

            MatUtils.checkDimsForUniformity(a, b);
        });
    }

    @Test
    public void testMultiArrayUniformity3() {
        assertThrows(DimensionMismatchException.class, () -> {
            double[][] a = new double[][]{
                new double[]{1, 7},
                new double[]{9, 8}
            };

            double[][] b = new double[][]{
                new double[]{1, 2, 4},
                new double[]{2, 1, 9}
            };

            MatUtils.checkDimsForUniformity(a, b);
        });
    }

    @Test
    public void testMultiArrayIntUniformity3() {
        assertThrows(DimensionMismatchException.class, () -> {
            int[][] a = new int[][]{
                new int[]{1, 7},
                new int[]{9, 8}
            };

            int[][] b = new int[][]{
                new int[]{1, 2, 4},
                new int[]{2, 1, 9}
            };

            MatUtils.checkDimsForUniformity(a, b);
        });
    }

    @Test
    public void testMultiArrayBooleanUniformity3() {
        assertThrows(DimensionMismatchException.class, () -> {
            boolean[][] a = new boolean[][]{
                new boolean[]{true, true},
                new boolean[]{true, true}
            };

            boolean[][] b = new boolean[][]{
                new boolean[]{false, false, false},
                new boolean[]{true, true, true}
            };

            MatUtils.checkDimsForUniformity(a, b);
        });
    }

    @Test
    public void testEqualsWithTolerance() {
        double[][] a = new double[][]{
            new double[]{0.00000000000000000000000001, 0}
        };
        double[][] b = new double[][]{
            new double[]{0, 0}
        };
        assertTrue(MatUtils.equalsWithTolerance(a, b));
        assertTrue(MatUtils.equalsWithTolerance(a, b, 1e-5));
        assertFalse(MatUtils.equalsWithTolerance(a, b, 1e-35));
    }

    @Test
    public void testIntEqualsExactly() {
        int[][] a = new int[][]{new int[]{1, 2, 4}};
        int[][] b = new int[][]{new int[]{1, 2, 3}};
        assertFalse(MatUtils.equalsExactly(a, b));
    }

    @Test
    public void testBooleanEqualsExactly() {
        boolean[][] a = new boolean[][]{new boolean[]{false, false, false}};
        boolean[][] b = new boolean[][]{new boolean[]{false, false, true}};
        assertFalse(MatUtils.equalsExactly(a, b));
    }

    @Test
    public void testFlatten1() {
        double[][] a = new double[][]{
            new double[]{1, 2, 3},
            new double[]{4, 5, 6}
        };

        assertTrue(VecUtils.equalsExactly(new double[]{1, 2, 3, 4, 5, 6}, MatUtils.flatten(a)));
    }

    @Test
    public void testFlatten2() {
        assertThrows(NonUniformMatrixException.class, () -> {
            double[][] a = new double[][]{
                new double[]{1, 2, 3},
                new double[]{4, 5}
            };

            MatUtils.flatten(a);
        });
    }

    @Test
    public void testFlatten3() {
        assertThrows(IllegalArgumentException.class, () -> {
            double[][] a = new double[][]{};
            MatUtils.flatten(a);
        });
    }

    @Test
    public void testIntFlatten1() {
        int[][] a = new int[][]{
            new int[]{1, 2, 3},
            new int[]{4, 5, 6}
        };

        assertTrue(VecUtils.equalsExactly(new int[]{1, 2, 3, 4, 5, 6}, MatUtils.flatten(a)));
    }

    @Test
    public void testIntFlatten2() {
        assertThrows(NonUniformMatrixException.class, () -> {
            int[][] a = new int[][]{
                new int[]{1, 2, 3},
                new int[]{4, 5}
            };

            MatUtils.flatten(a);
        });
    }

    @Test
    public void testIntFlatten3() {
        assertThrows(IllegalArgumentException.class, () -> {
            int[][] a = new int[][]{};
            MatUtils.flatten(a);
        });
    }

    @Test
    public void testFlattenUpperTriangular1() {
        double[][] a = new double[][]{
            new double[]{1, 2, 3},
            new double[]{4, 5, 6},
            new double[]{7, 8, 9}
        };

        assertTrue(VecUtils.equalsExactly(MatUtils
            .flattenUpperTriangularMatrix(a), new double[]{2, 3, 6}));
    }

    @Test
    public void testFlattenUpperTriangular2() {
        assertThrows(IllegalArgumentException.class, () -> {
            double[][] a = new double[][]{
            };

            MatUtils.flattenUpperTriangularMatrix(a);
        });
    }

    @Test
    public void testFlattenUpperTriangular3() {
        assertThrows(NonUniformMatrixException.class, () -> {
            double[][] a = new double[][]{
                new double[]{1, 2, 3},
                new double[]{4, 5},
                new double[]{7, 8, 9}
            };

            MatUtils.flattenUpperTriangularMatrix(a);
        });
    }

    @Test
    public void testFlattenUpperTriangular4() {
        assertThrows(DimensionMismatchException.class, () -> {
            double[][] a = new double[][]{
                new double[]{1, 2, 3},
                new double[]{4, 5, 6}
            };

            MatUtils.flattenUpperTriangularMatrix(a);
        });
    }

    @Test
    public void testFlooring1() {
        double[][] a = new double[][]{
            new double[]{1, 2, 3},
            new double[]{4, 5, 6}
        };

        assertTrue(MatUtils.equalsExactly(MatUtils.floor(a, 3, 0),
            new double[][]{
                new double[]{0, 0, 3},
                new double[]{4, 5, 6}
            }));
    }

    @Test
    public void testFlooring2() {
        double[][] a = new double[][]{
            new double[]{},
            new double[]{}
        };

        assertTrue(MatUtils.equalsExactly(MatUtils.floor(a, 3, 0),
            new double[][]{
                new double[]{},
                new double[]{}
            }));
    }

    @Test
    public void testFromVector2() {
        double[][] expected = new double[][]{
            new double[]{1, 1}
        };

        double[][] expected2 = new double[][]{
            new double[]{1},
            new double[]{1}
        };

        assertTrue(MatUtils.equalsExactly(expected,
            MatUtils.fromVector(new double[]{1}, 2, Axis.ROW)));
        assertTrue(MatUtils.equalsExactly(expected2,
            MatUtils.fromVector(new double[]{1}, 2, Axis.COL)));
    }

    @Test
    public void testFromVector3() {
        assertThrows(IllegalArgumentException.class, () -> {
            MatUtils.fromVector(new double[]{1}, 0, Axis.COL);
        });
    }

    @Test
    public void testFromList() {
        final ArrayList<double[]> in = new ArrayList<>();
        in.add(new double[]{1, 2, 3, 4});
        in.add(new double[]{});
        in.add(new double[]{1});

        final double[][] out = new double[][]{
            new double[]{1, 2, 3, 4},
            new double[]{},
            new double[]{1}
        };

        assertTrue(MatUtils.equalsExactly(out, MatUtils.fromList(in)));
    }

    @Test
    public void testGetColumn() {
        double[][] ad = new double[][]{
            new double[]{1, 2, 3},
            new double[]{4, 5, 6}
        };

        int[][] ai = new int[][]{
            new int[]{1, 2, 3},
            new int[]{4, 5, 6}
        };

        assertTrue(VecUtils.equalsExactly(new double[]{1, 4}, MatUtils.getColumn(ad, 0)));
        assertTrue(VecUtils.equalsExactly(new int[]{1, 4}, MatUtils.getColumn(ai, 0)));
        assertTrue(VecUtils.equalsExactly(new double[]{3, 6}, MatUtils.getColumn(ad, 2)));
        assertTrue(VecUtils.equalsExactly(new int[]{3, 6}, MatUtils.getColumn(ai, 2)));
    }

    @Test
    public void testGetColumnException1() {
        assertThrows(IndexOutOfBoundsException.class, () -> {
            MatUtils.getColumn(new double[][]{new double[]{}}, -1);
        });
    }

    @Test
    public void testGetColumnException2() {
        assertThrows(IndexOutOfBoundsException.class, () -> {
            MatUtils.getColumn(new int[][]{new int[]{}}, -1);
        });
    }

    @Test
    public void testGetColumnException3() {
        assertThrows(IndexOutOfBoundsException.class, () -> {
            MatUtils.getColumn(new double[][]{new double[]{1, 2, 3}}, 3);
        });
    }

    @Test
    public void testGetColumnException4() {
        assertThrows(IndexOutOfBoundsException.class, () -> {
            MatUtils.getColumn(new int[][]{new int[]{1, 2, 3}}, 3);
        });
    }

    @Test
    public void testGetColumnException5() {
        assertThrows(NonUniformMatrixException.class, () -> {
            MatUtils.getColumn(new double[][]{new double[]{1, 2, 3}, new double[]{1}}, 0);
        });
    }

    @Test
    public void testGetColumnException6() {
        assertThrows(NonUniformMatrixException.class, () -> {
            MatUtils.getColumn(new int[][]{new int[]{1, 2, 3}, new int[]{1}}, 0);
        });
    }

    @Test
    public void testGetColumns() {
        double[][] a = new double[][]{
            new double[]{0, 1, 2, 3},
            new double[]{4, 5, 6, 7},
            new double[]{8, 9, 10, 11}
        };

        assertTrue(MatUtils.equalsExactly(
            MatUtils.getColumns(a, new int[]{0, 0, 2}),
            new double[][]{
                new double[]{0, 0, 2},
                new double[]{4, 4, 6},
                new double[]{8, 8, 10}
            }));

        assertTrue(MatUtils.equalsExactly(
            MatUtils.getColumns(a, new Integer[]{0, 0, 2}),
            new double[][]{
                new double[]{0, 0, 2},
                new double[]{4, 4, 6},
                new double[]{8, 8, 10}
            }));
    }

    @Test
    public void testGetColumnsEmpty() {
        assertThrows(IllegalArgumentException.class, () -> {
            MatUtils.getColumns(new double[][]{}, new Integer[]{0, 0, 2});
        });
    }

    @Test
    public void testGetRows() {
        double[][] a = new double[][]{
            new double[]{0, 1, 2, 3},
            new double[]{4, 5, 6, 7},
            new double[]{8, 9, 10, 11}
        };

        assertTrue(MatUtils.equalsExactly(
            MatUtils.getRows(a, new int[]{0, 0, 2}),
            new double[][]{
                new double[]{0, 1, 2, 3},
                new double[]{0, 1, 2, 3},
                new double[]{8, 9, 10, 11}
            }));

        assertTrue(MatUtils.equalsExactly(
            MatUtils.getRows(a, new Integer[]{0, 0, 2}),
            new double[][]{
                new double[]{0, 1, 2, 3},
                new double[]{0, 1, 2, 3},
                new double[]{8, 9, 10, 11}
            }));
    }

    @Test
    public void testGetRowsEmpty() {
        assertThrows(IllegalArgumentException.class, () -> {
            MatUtils.getRows(new double[][]{}, new Integer[]{0, 0, 2});
        });
    }

    @Test
    public void testAxisMinMax() {
        double[][] a = new double[][]{
            new double[]{0, 1, 2, 3},
            new double[]{4, 5, 6, 7},
            new double[]{8, 9, 10, 11}
        };

        assertTrue(VecUtils.equalsExactly(MatUtils.max(a, Axis.COL), a[2]));
        assertTrue(VecUtils.equalsExactly(MatUtils.max(a, Axis.ROW),
            new double[]{3, 7, 11}));

        assertTrue(VecUtils.equalsExactly(MatUtils.min(a, Axis.COL), a[0]));
        assertTrue(VecUtils.equalsExactly(MatUtils.min(a, Axis.ROW),
            new double[]{0, 4, 8}));
    }

    @Test
    public void testMinMaxEmpty1() {
        assertThrows(IllegalArgumentException.class, () -> {
            MatUtils.min(new double[][]{}, Axis.ROW);
        });
    }

    @Test
    public void testMinMaxEmpty2() {
        assertThrows(IllegalArgumentException.class, () -> {
            MatUtils.min(new double[][]{}, Axis.COL);
        });
    }

    @Test
    public void testMinMaxEmpty3() {
        assertThrows(IllegalArgumentException.class, () -> {
            MatUtils.max(new double[][]{}, Axis.ROW);
        });
    }

    @Test
    public void testMinMaxEmpty4() {
        assertThrows(IllegalArgumentException.class, () -> {
            MatUtils.max(new double[][]{}, Axis.COL);
        });
    }

    @Test
    public void testMinMaxNUME1() {
        assertThrows(NonUniformMatrixException.class, () -> {
            double[][] a = new double[][]{
                new double[]{1, 2},
                new double[]{},
                new double[]{1}
            };

            MatUtils.min(a, Axis.ROW);
        });
    }

    @Test
    public void testMinMaxNUME2() {
        assertThrows(NonUniformMatrixException.class, () -> {
            double[][] a = new double[][]{
                new double[]{1, 2},
                new double[]{},
                new double[]{1}
            };

            MatUtils.min(a, Axis.COL);
        });
    }

    @Test
    public void testMinMaxNUME3() {
        assertThrows(NonUniformMatrixException.class, () -> {
            double[][] a = new double[][]{
                new double[]{1, 2},
                new double[]{},
                new double[]{1}
            };

            MatUtils.max(a, Axis.ROW);
        });
    }

    @Test
    public void testMinMaxNUME() {
        assertThrows(NonUniformMatrixException.class, () -> {
            double[][] a = new double[][]{
                new double[]{1, 2},
                new double[]{},
                new double[]{1}
            };

            MatUtils.max(a, Axis.COL);
        });
    }

    @Test
    public void testMeanRecords() {
        assertTrue(VecUtils.equalsWithTolerance(
            MatUtils.meanRecord(IRIS.getData().getData()),
            new double[]{5.84333333, 3.054, 3.75866667, 1.19866667}, 1e-8));

        double[][] d = new double[][]{
            new double[]{0, 1, 2},
            new double[]{2, 1, 0},
            new double[]{1.75, 1.75, 1.75}
        };

        assertTrue(VecUtils.equalsExactly(MatUtils.meanRecord(d), new double[]{1.25, 1.25, 1.25}));
        assertTrue(VecUtils.equalsExactly(
            MatUtils.meanRecord(new double[][]{new double[]{}, new double[]{}}),
            new double[]{}));
    }

    @Test
    public void testMeanRecordIAE() {
        assertThrows(IllegalArgumentException.class, () -> {
            MatUtils.meanRecord(new double[][]{});
        });
    }

    @Test
    public void testMeanRecordNUME() {
        assertThrows(NonUniformMatrixException.class, () -> {
            MatUtils.meanRecord(new double[][]{
                new double[]{},
                new double[]{1, 2, 3}
            });
        });
    }

    @Test
    public void testMedianRecord() {
        assertTrue(VecUtils.equalsExactly(
            MatUtils.medianRecord(IRIS.getData().getData()),
            new double[]{5.8, 3.0, 4.35, 1.3}));

        double[][] d = new double[][]{
            new double[]{0, 1, 2},
            new double[]{2, 1, 0},
            new double[]{1.75, 1.75, 1.75}
        };

        assertTrue(VecUtils.equalsExactly(
            MatUtils.medianRecord(d),
            new double[]{1.75, 1, 1.75}));
        assertTrue(VecUtils.equalsExactly(
            MatUtils.medianRecord(new double[][]{new double[]{}, new double[]{}}),
            new double[]{}));
    }

    @Test
    public void testMedianRecordIAE() {
        assertThrows(IllegalArgumentException.class, () -> {
            MatUtils.medianRecord(new double[][]{});
        });
    }

    @Test
    public void testMedianRecordNUME() {
        assertThrows(NonUniformMatrixException.class, () -> {
            MatUtils.medianRecord(new double[][]{
                new double[]{},
                new double[]{1, 2, 3}
            });
        });
    }

    @Test
    public void testMultiplication() {
        double[][] a = new double[][]{
            new double[]{0.2364806, 0.7345841, 0.28644998, 0.73868295, 0.73302448},
            new double[]{0.55749876, 0.13274999, 0.26477233, 0.92365331, 0.91806343},
            new double[]{0.6493526, 0.12678364, 0.67091553, 0.34491586, 0.86128125},
            new double[]{0.47833579, 0.57711398, 0.87922115, 0.34463673, 0.11473969}
        };

        double[][] b = new double[][]{
            new double[]{0.28103802, 0.35056847},
            new double[]{0.86042456, 0.82745279},
            new double[]{0.19988524, 0.61973525},
            new double[]{0.19302864, 0.92796937},
            new double[]{0.84613975, 0.43899853}
        };

        double[][] product = new double[][]{
            new double[]{1.5185994790989712, 1.8755312760443719},
            new double[]{1.2789252835311595, 1.7295250594395744},
            new double[]{1.2210295793067456, 1.4465125838040807},
            new double[]{0.9703474881065354, 1.5602912426056987}
        };

        assertTrue(MatUtils.equalsExactly(MatUtils.multiply(a, b), product));


        // Force a massively distributed task... can take long time (if works...)!
        if (GlobalState.ParallelismConf.PARALLELISM_ALLOWED) {
            assertTrue(MatUtils.equalsExactly(MatUtils.multiplyDistributed(a, b), product));
        }
    }

    @Test
    public void testMultDimExcept1() {
        assertThrows(DimensionMismatchException.class, () -> {
            MatUtils.multiply(
                new double[][]{
                    new double[]{1, 2, 3},
                    new double[]{1, 2, 3}
                },

                new double[][]{
                    new double[]{1, 2, 3},
                    new double[]{1, 2, 3}
                });
        });
    }

    @Test
    public void testMultDimExcept2() {
        assertThrows(DimensionMismatchException.class, () -> {
            MatUtils.multiplyDistributed(
                new double[][]{
                    new double[]{1, 2, 3},
                    new double[]{1, 2, 3}
                },

                new double[][]{
                    new double[]{1, 2, 3},
                    new double[]{1, 2, 3}
                });
        });
    }

    @Test
    public void testMultDimExcept3() {
        assertThrows(DimensionMismatchException.class, () -> {
            MatUtils.multiply(
                TestSuite.getRandom(10, 2).getDataRef(),
                TestSuite.getRandom(10, 2).getDataRef());
        });
    }

    @Test
    public void testMultIAExcept1() {
        assertThrows(IllegalArgumentException.class, () -> {
            MatUtils.multiply(
                new double[][]{
                },

                new double[][]{
                });
        });
    }

    @Test
    public void testMultIAExcept2() {
        assertThrows(IllegalArgumentException.class, () -> {
            MatUtils.multiplyDistributed(
                new double[][]{
                },

                new double[][]{
                });
        });
    }

    @Test
    public void testNegative1() {
        assertTrue(MatUtils.equalsExactly(
            MatUtils.negative(new double[][]{
                new double[]{}
            }),
            new double[][]{
                new double[]{}
            }));
    }

    @Test
    public void testNegative2() {
        assertTrue(MatUtils.equalsExactly(
            MatUtils.negative(new double[][]{
                new double[]{-0, 1, 2},
                new double[]{-2, 1, 0},
                new double[]{-5, 2, 3}
            }),
            new double[][]{
                new double[]{0, -1, -2},
                new double[]{2, -1, -0},
                new double[]{5, -2, -3}
            }));
    }

    @Test
    public void testNegativeIAE() {
        assertThrows(IllegalArgumentException.class, () -> {
            MatUtils.negative(new double[][]{});
        });
    }

    @Test
    public void testRandomGaussians() {
        double[][] a = MatUtils.randomGaussian(4, 4);
        assertTrue(a.length == 4 && a[0].length == 4);
    }

    @Test
    public void testRandomGaussiansEmpty() {
        assertTrue(MatUtils.equalsExactly(MatUtils.randomGaussian(0, 0),
            new double[][]{}));
    }

    @Test
    public void testRandomGaussiansIAE1() {
        assertThrows(IllegalArgumentException.class, () -> {
            MatUtils.randomGaussian(0, -1);
        });
    }

    @Test
    public void testRandomGaussiansIAE2() {
        assertThrows(IllegalArgumentException.class, () -> {
            MatUtils.randomGaussian(-1, 0);
        });
    }

    @Test
    public void testReorder() {
        double[][] a = new double[][]{
            new double[]{0, 0, 0},
            new double[]{1, 1, 1}
        };

        int[][] b = new int[][]{
            new int[]{0, 0, 0},
            new int[]{1, 1, 1}
        };

        assertTrue(MatUtils.equalsExactly(
            MatUtils.reorder(
                a, new int[]{0, 0, 0}),

            new double[][]{
                new double[]{0, 0, 0},
                new double[]{0, 0, 0},
                new double[]{0, 0, 0}
            }));

        assertTrue(MatUtils.equalsExactly(
            MatUtils.reorder(
                b, new int[]{0, 0, 0}),

            new int[][]{
                new int[]{0, 0, 0},
                new int[]{0, 0, 0},
                new int[]{0, 0, 0}
            }));
    }

    @Test
    public void testReorderIAE1() {
        assertThrows(IllegalArgumentException.class, () -> {
            MatUtils.reorder(new int[][]{}, new int[]{0, 0, 0});
        });
    }

    @Test
    public void testReorderIAE2() {
        assertThrows(IllegalArgumentException.class, () -> {
            MatUtils.reorder(new int[][]{new int[]{}}, new int[]{});
        });
    }

    @Test
    public void testReorderIAE3() {
        assertThrows(IllegalArgumentException.class, () -> {
            MatUtils.reorder(new double[][]{}, new int[]{0, 0, 0});
        });
    }

    @Test
    public void testReorderIAE4() {
        assertThrows(IllegalArgumentException.class, () -> {
            MatUtils.reorder(new double[][]{new double[]{}}, new int[]{});
        });
    }

    @Test
    public void testReshape1() {
        final double[][] a = new double[][]{
            new double[]{-0, 1, 2, 3},
            new double[]{-2, 1, 0, 8},
            new double[]{-5, 2, 3, 4}
        };

        final double[][] b = new double[][]{
            new double[]{-0, 1, 2},
            new double[]{3, -2, 1},
            new double[]{0, 8, -5},
            new double[]{2, 3, 4}
        };

        assertTrue(MatUtils.equalsExactly(MatUtils.reshape(a, 4, 3), b));
    }

    @Test
    public void testReshapeNUME1() {
        assertThrows(NonUniformMatrixException.class, () -> {
            MatUtils.reshape(new double[][]{
                new double[]{-0, 1, 2, 3},
                new double[]{-2, 1, 8},
                new double[]{-5, 2, 3, 4}
            }, 4, 3);
        });
    }

    @Test
    public void testReshapeIAE1() {
        assertThrows(IllegalArgumentException.class, () -> {
            MatUtils.reshape(new double[][]{
                new double[]{-0, 1, 2, 3},
                new double[]{-2, 1, 8, 2},
                new double[]{-5, 2, 3, 4}
            }, 4, 2);
        });
    }

    @Test
    public void testReshapeIAE2() {
        assertThrows(IllegalArgumentException.class, () -> {
            MatUtils.reshape(new double[][]{
                new double[]{-0, 1, 2, 3},
                new double[]{-2, 1, 8, 2},
                new double[]{-5, 2, 3, 4}
            }, -12, -1);
        });
    }

    @Test
    public void testReshapeIAE2_5() {
        assertThrows(IllegalArgumentException.class, () -> {
            MatUtils.reshape(new int[][]{
                new int[]{-0, 1, 2, 3},
                new int[]{-2, 1, 8, 2},
                new int[]{-5, 2, 3, 4}
            }, -12, -1);
        });
    }

    @Test
    public void testReshapeIAE2_75() {
        assertThrows(IllegalArgumentException.class, () -> {
            MatUtils.reshape(new int[][]{
                new int[]{-0, 1, 2, 3},
                new int[]{-2, 1, 8, 2},
                new int[]{-5, 2, 3, 4}
            }, 11, 1);
        });
    }

    @Test
    public void testReshapeIAE3() {
        assertThrows(IllegalArgumentException.class, () -> {
            final double[] n = new double[]{1, 2, 3, 4, 5};
            MatUtils.reshape(n, 3, 2); //here
        }); //here
    }

    @Test
    public void testReshapeIAE4() {
        assertThrows(IllegalArgumentException.class, () -> {
            final int[] n = new int[]{1, 2, 3, 4, 5};
            MatUtils.reshape(n, 3, 2); //here
        }); //here
    }

    @Test
    public void testReshapeIAE5() {
        assertThrows(IllegalArgumentException.class, () -> {
            final double[] n = new double[]{1, 2, 3, 4, 5, 6};
            MatUtils.reshape(n, -3, -2); //here
        }); //here
    }

    @Test
    public void testReshapeIAE6() {
        assertThrows(IllegalArgumentException.class, () -> {
            final int[] n = new int[]{1, 2, 3, 4, 5, 6};
            MatUtils.reshape(n, -3, -2); //here
        }); //here
    }

    public void testRepping() {
        double val = 3.0;
        assertTrue(MatUtils.equalsExactly(
            MatUtils.rep(val, 3, 3),
            new double[][]{
                new double[]{val, val, val},
                new double[]{val, val, val},
                new double[]{val, val, val}
            }));

        assertTrue(MatUtils.equalsExactly(
            MatUtils.rep(new double[]{val, val, val}, 3),
            new double[][]{
                new double[]{val, val, val},
                new double[]{val, val, val},
                new double[]{val, val, val}
            }));

        assertTrue(MatUtils.equalsExactly(
            MatUtils.rep(val, 2, 0),
            new double[][]{
                new double[]{},
                new double[]{}
            }));
    }

    @Test
    public void testRepIAE1() {
        assertThrows(IllegalArgumentException.class, () -> {
            MatUtils.rep(3.0, -1, 0);
        });
    }

    @Test
    public void testRepIAE2() {
        assertThrows(IllegalArgumentException.class, () -> {
            MatUtils.rep(3.0, 0, -1);
        });
    }

    @Test
    public void testRepIAE3() {
        assertThrows(IllegalArgumentException.class, () -> {
            MatUtils.rep(3.0, 0, 0);
        });
    }

    @Test
    public void testRepIAE4() {
        assertThrows(IllegalArgumentException.class, () -> {
            MatUtils.rep(new double[]{1.0, 1.0}, -1);
        });
    }

    @Test
    public void testRepIAE5() {
        assertThrows(IllegalArgumentException.class, () -> {
            MatUtils.rep(new double[]{1.0, 1.0}, 0);
        });
    }

    @Test
    public void testRowMeansSumIAE1() {
        assertThrows(IllegalArgumentException.class, () -> {
            MatUtils.rowSums(new double[][]{});
        });
    }

    @Test
    public void testRowMeansSumIAE2() {
        assertThrows(IllegalArgumentException.class, () -> {
            MatUtils.rowMeans(new double[][]{});
        });
    }

    @Test
    public void testScalarOps() {
        double val = 1.0;
        double[][] a = new double[][]{
            new double[]{4.0, 2.0, 0.0},
            new double[]{1.5, 3.2, 1.1}
        };

        // Subtraction yields some extremely small floating point difference
        // for 0.1: 0.10000000000000009
        assertTrue(MatUtils.equalsWithTolerance(
            MatUtils.scalarSubtract(a, val),
            new double[][]{
                new double[]{3.0, 1.0, -1.0},
                new double[]{0.5, 2.2, 0.1}
            }, 1e-9));

        assertTrue(MatUtils.equalsExactly(
            MatUtils.scalarDivide(a, val),
            new double[][]{
                new double[]{4.0, 2.0, 0.0},
                new double[]{1.5, 3.2, 1.1}
            }));

        assertTrue(MatUtils.equalsExactly(
            MatUtils.scalarMultiply(a, val),
            new double[][]{
                new double[]{4.0, 2.0, 0.0},
                new double[]{1.5, 3.2, 1.1}
            }));

        assertTrue(MatUtils.equalsExactly(
            MatUtils.scalarAdd(a, val),
            new double[][]{
                new double[]{5.0, 3.0, 1.0},
                new double[]{2.5, 4.2, 2.1}
            }));


        // Test non-uniform matrices...
        a = new double[][]{
            new double[]{4.0},
            new double[]{1.5, 3.2, 1.1}
        };

        assertTrue(MatUtils.equalsWithTolerance(
            MatUtils.scalarSubtract(a, val),
            new double[][]{
                new double[]{3.0},
                new double[]{0.5, 2.2, 0.1}
            }, 1e-9));

        assertTrue(MatUtils.equalsExactly(
            MatUtils.scalarDivide(a, val),
            new double[][]{
                new double[]{4.0},
                new double[]{1.5, 3.2, 1.1}
            }));

        assertTrue(MatUtils.equalsExactly(
            MatUtils.scalarMultiply(a, val),
            new double[][]{
                new double[]{4.0},
                new double[]{1.5, 3.2, 1.1}
            }));

        assertTrue(MatUtils.equalsExactly(
            MatUtils.scalarAdd(a, val),
            new double[][]{
                new double[]{5.0},
                new double[]{2.5, 4.2, 2.1}
            }));


        // Test empty matrices....
        a = new double[][]{
            new double[]{},
            new double[]{}
        };

        assertTrue(MatUtils.equalsExactly(
            MatUtils.scalarSubtract(a, val),
            a));

        assertTrue(MatUtils.equalsExactly(
            MatUtils.scalarDivide(a, val),
            a));

        assertTrue(MatUtils.equalsExactly(
            MatUtils.scalarMultiply(a, val),
            a));

        assertTrue(MatUtils.equalsExactly(
            MatUtils.scalarAdd(a, val),
            a));
    }

    @Test
    public void testScalarOpIAE1() {
        assertThrows(IllegalArgumentException.class, () -> {
            MatUtils.scalarAdd(new double[][]{}, 0.0);
        });
    }

    @Test
    public void testScalarOpIAE2() {
        assertThrows(IllegalArgumentException.class, () -> {
            MatUtils.scalarDivide(new double[][]{}, 0.0);
        });
    }

    @Test
    public void testScalarOpIAE3() {
        assertThrows(IllegalArgumentException.class, () -> {
            MatUtils.scalarMultiply(new double[][]{}, 0.0);
        });
    }

    @Test
    public void testScalarOpIAE4() {
        assertThrows(IllegalArgumentException.class, () -> {
            MatUtils.scalarSubtract(new double[][]{}, 0.0);
        });
    }

    public void testScalarVectorOps() {
        double[] valCol = new double[]{1.0, 2.0, 3.0};
        double[] valRow = new double[]{1.0, 2.0};
        double[] emptyVec = new double[]{};

        double[][] a = new double[][]{
            new double[]{4.0, 2.0, 0.0},
            new double[]{1.5, 3.2, 1.1}
        };

        final double[][] empty = new double[][]{
            new double[]{},
            new double[]{}
        };


        // COL AXIS TESTS ==========================

        assertTrue(MatUtils.equalsExactly(
            MatUtils.scalarAdd(a, valCol, Axis.COL),
            new double[][]{
                new double[]{5.0, 4.0, 3.0},
                new double[]{2.5, 5.2, 4.1}
            }));

        assertTrue(MatUtils.equalsExactly(
            MatUtils.scalarDivide(a, valCol, Axis.COL),
            new double[][]{
                new double[]{4.0, 1.0, 0.0},
                new double[]{1.5, 1.6, (1.1 / 3.0)}
            }));

        assertTrue(MatUtils.equalsExactly(
            MatUtils.scalarMultiply(a, valCol, Axis.COL),
            new double[][]{
                new double[]{4.0, 4.0, 0.0},
                new double[]{1.5, 6.4, 3.3}
            }));

        assertTrue(MatUtils.equalsExactly(
            MatUtils.scalarSubtract(a, valCol, Axis.COL),
            new double[][]{
                new double[]{3.0, 0.0, -3.0},
                new double[]{0.5, 1.2, -2.1}
            }));

        // test empty matrices
        assertTrue(MatUtils.equalsExactly(
            MatUtils.scalarAdd(empty, emptyVec, Axis.COL),
            empty));

        assertTrue(MatUtils.equalsExactly(
            MatUtils.scalarDivide(empty, emptyVec, Axis.COL),
            empty));

        assertTrue(MatUtils.equalsExactly(
            MatUtils.scalarMultiply(empty, emptyVec, Axis.COL),
            empty));

        assertTrue(MatUtils.equalsExactly(
            MatUtils.scalarSubtract(empty, emptyVec, Axis.COL),
            empty));

        // test empty matrices with dim mismatched vectors
        boolean pass = false;

        try {
            pass = false;
            MatUtils.scalarAdd(empty, valCol, Axis.COL);
        } catch (DimensionMismatchException e) {
            pass = true;
        } finally {
            if (!pass)
                fail();
        }

        try {
            pass = false;
            MatUtils.scalarDivide(empty, valCol, Axis.COL);
        } catch (DimensionMismatchException e) {
            pass = true;
        } finally {
            if (!pass)
                fail();
        }

        try {
            pass = false;
            MatUtils.scalarMultiply(empty, valCol, Axis.COL);
        } catch (DimensionMismatchException e) {
            pass = true;
        } finally {
            if (!pass)
                fail();
        }

        try {
            pass = false;
            MatUtils.scalarSubtract(empty, valCol, Axis.COL);
        } catch (DimensionMismatchException e) {
            pass = true;
        } finally {
            if (!pass)
                fail();
        }


        // ROW AXIS TESTS ==========================
        // row = {1.0,2.0}
        // mat = [{4.0,2.0,0.0},
        //        {1.5,3.2,1.1}]

        assertTrue(MatUtils.equalsExactly(
            MatUtils.scalarAdd(a, valRow, Axis.ROW),
            new double[][]{
                new double[]{5.0, 3.0, 1.0},
                new double[]{3.5, 5.2, 3.1}
            }));

        assertTrue(MatUtils.equalsExactly(
            MatUtils.scalarDivide(a, valRow, Axis.ROW),
            new double[][]{
                new double[]{4.0, 2.0, 0.0},
                new double[]{0.75, 1.6, 0.55}
            }));

        assertTrue(MatUtils.equalsExactly(
            MatUtils.scalarMultiply(a, valRow, Axis.ROW),
            new double[][]{
                new double[]{4.0, 2.0, 0.0},
                new double[]{3.0, 6.4, 2.2}
            }));

        assertTrue(MatUtils.equalsExactly(
            MatUtils.scalarSubtract(a, valRow, Axis.ROW),
            new double[][]{
                new double[]{3.0, 1.0, -1.0},
                new double[]{-0.5, 1.2, -1.1}
            }));

        // test empty matrices
        assertTrue(MatUtils.equalsExactly(
            MatUtils.scalarAdd(empty, emptyVec, Axis.ROW),
            empty));

        assertTrue(MatUtils.equalsExactly(
            MatUtils.scalarDivide(empty, emptyVec, Axis.ROW),
            empty));

        assertTrue(MatUtils.equalsExactly(
            MatUtils.scalarMultiply(empty, emptyVec, Axis.ROW),
            empty));

        assertTrue(MatUtils.equalsExactly(
            MatUtils.scalarSubtract(empty, emptyVec, Axis.ROW),
            empty));

        // test empty matrices with dim mismatched vectors
        pass = false;

        try {
            pass = false;
            MatUtils.scalarAdd(empty, valRow, Axis.ROW);
        } catch (DimensionMismatchException e) {
            pass = true;
        } finally {
            if (!pass)
                fail();
        }

        try {
            pass = false;
            MatUtils.scalarDivide(empty, valRow, Axis.ROW);
        } catch (DimensionMismatchException e) {
            pass = true;
        } finally {
            if (!pass)
                fail();
        }

        try {
            pass = false;
            MatUtils.scalarMultiply(empty, valRow, Axis.ROW);
        } catch (DimensionMismatchException e) {
            pass = true;
        } finally {
            if (!pass)
                fail();
        }

        try {
            pass = false;
            MatUtils.scalarSubtract(empty, valRow, Axis.ROW);
        } catch (DimensionMismatchException e) {
            pass = true;
        } finally {
            if (!pass)
                fail();
        }
    }

    // Tests on empty matrices
    @Test
    public void testScalarVecOpIAE1() {
        assertThrows(IllegalArgumentException.class, () -> {
            MatUtils.scalarAdd(new double[][]{}, new double[]{}, Axis.ROW);
        });
    }

    @Test
    public void testScalarVecOpIAE2() {
        assertThrows(IllegalArgumentException.class, () -> {
            MatUtils.scalarDivide(new double[][]{}, new double[]{}, Axis.ROW);
        });
    }

    @Test
    public void testScalarVecOpIAE3() {
        assertThrows(IllegalArgumentException.class, () -> {
            MatUtils.scalarMultiply(new double[][]{}, new double[]{}, Axis.ROW);
        });
    }

    @Test
    public void testScalarVecOpIAE4() {
        assertThrows(IllegalArgumentException.class, () -> {
            MatUtils.scalarSubtract(new double[][]{}, new double[]{}, Axis.ROW);
        });
    }

    @Test
    public void testScalarColVecOpIAE1() {
        assertThrows(IllegalArgumentException.class, () -> {
            MatUtils.scalarAdd(new double[][]{}, new double[]{}, Axis.COL);
        });
    }

    @Test
    public void testScalarColVecOpIAE2() {
        assertThrows(IllegalArgumentException.class, () -> {
            MatUtils.scalarDivide(new double[][]{}, new double[]{}, Axis.COL);
        });
    }

    @Test
    public void testScalarColVecOpIAE3() {
        assertThrows(IllegalArgumentException.class, () -> {
            MatUtils.scalarMultiply(new double[][]{}, new double[]{}, Axis.COL);
        });
    }

    @Test
    public void testScalarColVecOpIAE4() {
        assertThrows(IllegalArgumentException.class, () -> {
            MatUtils.scalarSubtract(new double[][]{}, new double[]{}, Axis.COL);
        });
    }

    // Tests on non-uniform matrices
    @Test
    public void testScalarVecOpNUME1() {
        assertThrows(NonUniformMatrixException.class, () -> {
            MatUtils.scalarAdd(new double[][]{
                new double[]{1, 2, 3},
                new double[]{1}
            }, new double[]{}, Axis.ROW);
        });
    }

    @Test
    public void testScalarVecOpNUME2() {
        assertThrows(NonUniformMatrixException.class, () -> {
            MatUtils.scalarDivide(new double[][]{
                new double[]{1, 2, 3},
                new double[]{1}
            }, new double[]{}, Axis.ROW);
        });
    }

    @Test
    public void testScalarVecOpNUME3() {
        assertThrows(NonUniformMatrixException.class, () -> {
            MatUtils.scalarMultiply(new double[][]{
                new double[]{1, 2, 3},
                new double[]{1}
            }, new double[]{}, Axis.ROW);
        });
    }

    @Test
    public void testScalarVecOpNUME4() {
        assertThrows(NonUniformMatrixException.class, () -> {
            MatUtils.scalarSubtract(new double[][]{
                new double[]{1, 2, 3},
                new double[]{1}
            }, new double[]{}, Axis.ROW);
        });
    }

    @Test
    public void testScalarColVecOpNUME1() {
        assertThrows(NonUniformMatrixException.class, () -> {
            MatUtils.scalarAdd(new double[][]{
                new double[]{1, 2, 3},
                new double[]{1}
            }, new double[]{}, Axis.COL);
        });
    }

    @Test
    public void testScalarColVecOpNUME2() {
        assertThrows(NonUniformMatrixException.class, () -> {
            MatUtils.scalarDivide(new double[][]{
                new double[]{1, 2, 3},
                new double[]{1}
            }, new double[]{}, Axis.COL);
        });
    }

    @Test
    public void testScalarColVecOpNUME3() {
        assertThrows(NonUniformMatrixException.class, () -> {
            MatUtils.scalarMultiply(new double[][]{
                new double[]{1, 2, 3},
                new double[]{1}
            }, new double[]{}, Axis.COL);
        });
    }

    @Test
    public void testScalarColVecOpNUME4() {
        assertThrows(NonUniformMatrixException.class, () -> {
            MatUtils.scalarSubtract(new double[][]{
                new double[]{1, 2, 3},
                new double[]{1}
            }, new double[]{}, Axis.COL);
        });
    }

    @Test
    public void testSetColInPlace() {
        double[][] a = new double[][]{
            new double[]{0, 1, 2},
            new double[]{2, 3, 1}
        };

        double[] v = new double[]{0, 1};


        MatUtils.setColumnInPlace(a, 0, v);
        assertTrue(MatUtils.equalsExactly(a,
            new double[][]{
                new double[]{0, 1, 2},
                new double[]{1, 3, 1}
            }));

        MatUtils.setColumnInPlace(a, 1, v);
        assertTrue(MatUtils.equalsExactly(a,
            new double[][]{
                new double[]{0, 0, 2},
                new double[]{1, 1, 1}
            }));

        MatUtils.setColumnInPlace(a, 2, v);
        assertTrue(MatUtils.equalsExactly(a,
            new double[][]{
                new double[]{0, 0, 0},
                new double[]{1, 1, 1}
            }));
    }

    @Test
    public void testSetColInPlaceIOOBE1() {
        assertThrows(IndexOutOfBoundsException.class, () -> {
            MatUtils.setColumnInPlace(new double[][]{
                new double[]{0, 1, 2},
                new double[]{0, 1, 2}}, 3, new double[]{});
        });
    }

    @Test
    public void testSetColInPlaceIOOBE2() {
        assertThrows(IndexOutOfBoundsException.class, () -> {
            MatUtils.setColumnInPlace(new double[][]{
                new double[]{0, 1, 2},
                new double[]{0, 1, 2}}, -1, new double[]{});
        });
    }

    @Test
    public void testSetColInPlaceIAE1() {
        assertThrows(IllegalArgumentException.class, () -> {
            MatUtils.setColumnInPlace(new double[][]{}, 0, new double[]{});
        });
    }

    @Test
    public void testSetColInPlaceNUME1() {
        assertThrows(NonUniformMatrixException.class, () -> {
            MatUtils.setColumnInPlace(new double[][]{
                new double[]{0, 1, 2},
                new double[]{0, 1}}, 0, new double[]{});
        });
    }

    @Test
    public void testSetColInPlaceDME1() {
        assertThrows(DimensionMismatchException.class, () -> {
            MatUtils.setColumnInPlace(new double[][]{
                new double[]{0, 1, 2},
                new double[]{0, 1, 2}}, 0, new double[]{1, 2, 3});
        });
    }

    @Test
    public void testSetRowInPlace() {
        double[][] a = new double[][]{
            new double[]{0, 1, 2},
            new double[]{2, 3, 1}
        };

        double[] v = new double[]{0, 1, 3};


        MatUtils.setRowInPlace(a, 0, v);

        assertTrue(MatUtils.equalsExactly(a,
            new double[][]{
                new double[]{0, 1, 3},
                new double[]{2, 3, 1}
            }));

        MatUtils.setRowInPlace(a, 1, v);
        assertTrue(MatUtils.equalsExactly(a,
            new double[][]{
                new double[]{0, 1, 3},
                new double[]{0, 1, 3}
            }));
    }

    @Test
    public void testSetRowIAE() {
        assertThrows(IllegalArgumentException.class, () -> {
            MatUtils.setRowInPlace(new double[][]{}, 0, new double[]{});
        });
    }

    @Test
    public void testSetRowIOOBE1() {
        assertThrows(IndexOutOfBoundsException.class, () -> {
            MatUtils.setRowInPlace(new double[][]{new double[]{}}, -1, new double[]{});
        });
    }

    @Test
    public void testSetRowIOOBE2() {
        assertThrows(IndexOutOfBoundsException.class, () -> {
            MatUtils.setRowInPlace(new double[][]{new double[]{}}, 1, new double[]{});
        });
    }

    @Test
    public void testSetRowDME() {
        assertThrows(DimensionMismatchException.class, () -> {
            MatUtils.setRowInPlace(new double[][]{new double[]{}}, 0, new double[]{1, 2, 3});
        });
    }


    @Test
    public void testSortAscDesc() {
        double[][] d = new double[][]{
            new double[]{2, 9, 3},
            new double[]{1, 2, 4},
            new double[]{0, 4, 5}
        };

        int[][] i = new int[][]{
            new int[]{2, 9, 3},
            new int[]{1, 2, 4},
            new int[]{0, 4, 5}
        };


        // asc tests
        assertTrue(MatUtils.equalsExactly(
            MatUtils.sortAscByCol(d, 0),
            new double[][]{
                new double[]{0, 4, 5},
                new double[]{1, 2, 4},
                new double[]{2, 9, 3}
            }));

        assertTrue(MatUtils.equalsExactly(
            MatUtils.sortAscByCol(i, 0),
            new int[][]{
                new int[]{0, 4, 5},
                new int[]{1, 2, 4},
                new int[]{2, 9, 3}
            }));

        assertTrue(MatUtils.equalsExactly(
            MatUtils.sortAscByCol(d, 1),
            new double[][]{
                new double[]{1, 2, 4},
                new double[]{0, 4, 5},
                new double[]{2, 9, 3}
            }));

        assertTrue(MatUtils.equalsExactly(
            MatUtils.sortAscByCol(i, 1),
            new int[][]{
                new int[]{1, 2, 4},
                new int[]{0, 4, 5},
                new int[]{2, 9, 3}
            }));

        // Ensure originals not affected
        assertTrue(d[0][0] != 1);
        assertTrue(i[0][0] != 1);


        // desc tests
        assertTrue(MatUtils.equalsExactly(
            MatUtils.sortDescByCol(d, 0),
            new double[][]{
                new double[]{2, 9, 3},
                new double[]{1, 2, 4},
                new double[]{0, 4, 5}
            }));

        assertTrue(MatUtils.equalsExactly(
            MatUtils.sortDescByCol(i, 0),
            new int[][]{
                new int[]{2, 9, 3},
                new int[]{1, 2, 4},
                new int[]{0, 4, 5}
            }));

        assertTrue(MatUtils.equalsExactly(
            MatUtils.sortDescByCol(d, 1),
            new double[][]{
                new double[]{2, 9, 3},
                new double[]{0, 4, 5},
                new double[]{1, 2, 4}
            }));

        assertTrue(MatUtils.equalsExactly(
            MatUtils.sortDescByCol(i, 1),
            new int[][]{
                new int[]{2, 9, 3},
                new int[]{0, 4, 5},
                new int[]{1, 2, 4}
            }));

        // Ensure originals not affected
        assertTrue(d[0][0] != 1);
        assertTrue(i[0][0] != 1);
    }

    @Test
    public void testSortIAE1() {
        assertThrows(IllegalArgumentException.class, () -> {
            MatUtils.sortAscByCol(new double[][]{}, 0);
        });
    }

    @Test
    public void testSortIAE2() {
        assertThrows(IllegalArgumentException.class, () -> {
            MatUtils.sortAscByCol(new int[][]{}, 0);
        });
    }

    @Test
    public void testSortIAE3() {
        assertThrows(IllegalArgumentException.class, () -> {
            MatUtils.sortDescByCol(new double[][]{}, 0);
        });
    }

    @Test
    public void testSortIAE4() {
        assertThrows(IllegalArgumentException.class, () -> {
            MatUtils.sortDescByCol(new int[][]{}, 0);
        });
    }

    @Test
    public void testSortIOOBE1() {
        assertThrows(IndexOutOfBoundsException.class, () -> {
            MatUtils.sortAscByCol(new double[][]{new double[]{}}, -1);
        });
    }

    @Test
    public void testSortIOOBE2() {
        assertThrows(IndexOutOfBoundsException.class, () -> {
            MatUtils.sortAscByCol(new double[][]{new double[]{}}, 1);
        });
    }

    @Test
    public void testSortIOOBE3() {
        assertThrows(IndexOutOfBoundsException.class, () -> {
            MatUtils.sortAscByCol(new int[][]{new int[]{}}, -1);
        });
    }

    @Test
    public void testSortIOOBE4() {
        assertThrows(IndexOutOfBoundsException.class, () -> {
            MatUtils.sortAscByCol(new int[][]{new int[]{}}, 1);
        });
    }

    @Test
    public void testSortIOOBE5() {
        assertThrows(IndexOutOfBoundsException.class, () -> {
            MatUtils.sortDescByCol(new double[][]{new double[]{}}, -1);
        });
    }

    @Test
    public void testSortIOOBE6() {
        assertThrows(IndexOutOfBoundsException.class, () -> {
            MatUtils.sortDescByCol(new double[][]{new double[]{}}, 1);
        });
    }

    @Test
    public void testSortIOOBE7() {
        assertThrows(IndexOutOfBoundsException.class, () -> {
            MatUtils.sortDescByCol(new int[][]{new int[]{}}, -1);
        });
    }

    @Test
    public void testSortIOOBE8() {
        assertThrows(IndexOutOfBoundsException.class, () -> {
            MatUtils.sortDescByCol(new int[][]{new int[]{}}, 1);
        });
    }

    @Test
    public void testSortNUME1() {
        assertThrows(NonUniformMatrixException.class, () -> {
            MatUtils.sortAscByCol(new double[][]{new double[]{}, new double[]{1, 2}}, 0);
        });
    }

    @Test
    public void testSortNUME2() {
        assertThrows(NonUniformMatrixException.class, () -> {
            MatUtils.sortAscByCol(new int[][]{new int[]{}, new int[]{1, 2}}, 0);
        });
    }

    @Test
    public void testSortNUME3() {
        assertThrows(NonUniformMatrixException.class, () -> {
            MatUtils.sortDescByCol(new double[][]{new double[]{}, new double[]{1, 2}}, 0);
        });
    }

    @Test
    public void testSortNUME4() {
        assertThrows(NonUniformMatrixException.class, () -> {
            MatUtils.sortDescByCol(new int[][]{new int[]{}, new int[]{1, 2}}, 0);
        });
    }

    @Test
    public void testSubtraction() {
        final double[][] a = new double[][]{
            new double[]{1.0, 2.0, 3.0},
            new double[]{3.0, 2.0, 1.0},
            new double[]{0.0, 0.0, 0.0}
        };

        final double[][] b = new double[][]{
            new double[]{0.0, 1.0, 2.0},
            new double[]{2.0, 1.0, 0.0},
            new double[]{-1.0, -1.0, -1.0}
        };

        final double[][] c = new double[][]{
            new double[]{1.0, 1.0, 1.0},
            new double[]{1.0, 1.0, 1.0},
            new double[]{1.0, 1.0, 1.0}
        };

        assertTrue(MatUtils.equalsExactly(
            MatUtils.subtract(a, b), c));

        // Check empty permitted
        assertTrue(MatUtils.equalsExactly(
            MatUtils.subtract(
                new double[][]{new double[]{}},
                new double[][]{new double[]{}}),

            new double[][]{new double[]{}}
        ));
    }

    @Test
    public void testSubIAE1() {
        assertThrows(IllegalArgumentException.class, () -> {
            MatUtils.subtract(new double[][]{}, new double[][]{new double[]{}});
        });
    }

    @Test
    public void testSubIAE2() {
        assertThrows(IllegalArgumentException.class, () -> {
            MatUtils.subtract(new double[][]{new double[]{}}, new double[][]{});
        });
    }

    @Test
    public void testSubDME1() {
        assertThrows(DimensionMismatchException.class, () -> {
            MatUtils.subtract(new double[][]{new double[]{}}, new double[][]{new double[]{}, new double[]{}});
        });
    }

    @Test
    public void testToDouble() {
        int[][] i = new int[][]{
            new int[]{0, 1},
            new int[]{2, 3}
        };

        assertTrue(MatUtils.equalsExactly(
            MatUtils.toDouble(i),
            new double[][]{
                new double[]{0, 1},
                new double[]{2, 3}
            }));

        // ensure empty works
        i = new int[][]{
            new int[]{},
            new int[]{}
        };

        assertTrue(MatUtils.equalsExactly(
            MatUtils.toDouble(i),
            new double[][]{
                new double[]{},
                new double[]{}
            }));

        // ensure jagged works
        i = new int[][]{
            new int[]{1, 0, 2},
            new int[]{}
        };

        assertTrue(MatUtils.equalsExactly(
            MatUtils.toDouble(i),
            new double[][]{
                new double[]{1, 0, 2},
                new double[]{}
            }));
    }

    @Test
    public void testToDoubleIAE() {
        assertThrows(IllegalArgumentException.class, () -> {
            MatUtils.toDouble(new int[][]{});
        });
    }

    @Test
    public void testTranspose() {
        double[][] a = new double[][]{
            new double[]{0, 1, 2},
            new double[]{3, 4, 5}
        };

        double[][] t = new double[][]{
            new double[]{0, 3},
            new double[]{1, 4},
            new double[]{2, 5}
        };

        assertTrue(MatUtils.equalsExactly(t, MatUtils.transpose(a)));
        assertTrue(MatUtils.equalsExactly(
            new double[][]{
                new double[]{0},
                new double[]{1}
            },

            MatUtils.transpose(new double[]{0, 1})));
    }

    @Test
    public void testTransposeIAE() {
        assertThrows(IllegalArgumentException.class, () -> {
            MatUtils.transpose(new double[][]{});
        });
    }

    @Test
    public void testTransposeNUME() {
        assertThrows(NonUniformMatrixException.class, () -> {
            MatUtils.transpose(new double[][]{new double[]{}, new double[]{1, 2}});
        });
    }

    @Test
    public void testTransposeEmpty() {
        assertThrows(IllegalArgumentException.class, () -> {
            MatUtils.transpose(new double[][]{new double[]{}});
        });
    }

    @Test
    public void testTransposeVecIAE() {
        assertThrows(IllegalArgumentException.class, () -> {
            MatUtils.transpose(new double[]{});
        });
    }

    @Test
    public void testScalarSubtractVector() {
        double[][] a = new double[][]{
            new double[]{0, 1, 2},
            new double[]{2, 3, 4}
        };

        double[] v = new double[]{-1, 2, 3}, y = new double[]{1, 2};
        double[][] b = MatUtils.scalarSubtract(a, v, Axis.COL);
        double[][] c = MatUtils.scalarSubtract(a, y, Axis.ROW);

        assertTrue(MatUtils.equalsExactly(b, new double[][]{
            new double[]{1, -1, -1},
            new double[]{3, 1, 1}
        }));

        assertTrue(MatUtils.equalsExactly(c, new double[][]{
            new double[]{-1, 0, 1},
            new double[]{0, 1, 2}
        }));
    }

    @Test
    public void testScalarAddVector() {
        double[][] a = new double[][]{
            new double[]{0, 1, 2},
            new double[]{2, 3, 4}
        };

        double[] v = new double[]{-1, 2, 3}, y = new double[]{1, 2};
        double[][] b = MatUtils.scalarAdd(a, v, Axis.COL);
        double[][] c = MatUtils.scalarAdd(a, y, Axis.ROW);

        assertTrue(MatUtils.equalsExactly(b, new double[][]{
            new double[]{-1, 3, 5},
            new double[]{1, 5, 7}
        }));

        assertTrue(MatUtils.equalsExactly(c, new double[][]{
            new double[]{1, 2, 3},
            new double[]{4, 5, 6}
        }));
    }

    @Test
    public void testScalarMultVector() {
        double[][] a = new double[][]{
            new double[]{0, 1, 2},
            new double[]{2, 3, 4}
        };

        double[] v = new double[]{-1, 2, 3}, y = new double[]{1, 2};
        double[][] b = MatUtils.scalarMultiply(a, v, Axis.COL);
        double[][] c = MatUtils.scalarMultiply(a, y, Axis.ROW);

        assertTrue(MatUtils.equalsExactly(b, new double[][]{
            new double[]{-0, 2, 6},
            new double[]{-2, 6, 12}
        }));

        assertTrue(MatUtils.equalsExactly(c, new double[][]{
            new double[]{0, 1, 2},
            new double[]{4, 6, 8}
        }));
    }

    @Test
    public void testScalarDivVector() {
        double[][] a = new double[][]{
            new double[]{0, 1, 2},
            new double[]{2, 3, 4}
        };

        double[] v = new double[]{-1, 2, 3}, y = new double[]{1, 2};
        double[][] b = MatUtils.scalarDivide(a, v, Axis.COL);
        double[][] c = MatUtils.scalarDivide(a, y, Axis.ROW);

        assertTrue(MatUtils.equalsExactly(b, new double[][]{
            new double[]{-0, 0.5, 2.0 / 3.0},
            new double[]{-2, 1.5, 4.0 / 3.0}
        }));

        assertTrue(MatUtils.equalsExactly(c, new double[][]{
            new double[]{0, 1, 2},
            new double[]{1, 1.5, 2}
        }));
    }

    @Test
    public void testScalarOpDME1() {
        assertThrows(DimensionMismatchException.class, () -> {
            double[][] a = new double[][]{
                new double[]{0, 1, 2},
                new double[]{2, 3, 4}
            };

            double[] v = new double[]{-1, 2, 3};
            MatUtils.scalarAdd(a, v, Axis.ROW); // here
        }); // here
    }

    @Test
    public void testScalarOpDME2() {
        assertThrows(DimensionMismatchException.class, () -> {
            double[][] a = new double[][]{
                new double[]{0, 1, 2},
                new double[]{2, 3, 4}
            };

            double[] v = new double[]{-1, 2};
            MatUtils.scalarAdd(a, v, Axis.COL); // here
        }); // here
    }

    @Test
    public void testBlockMatMult() {
        /*
         * This is not run in parallel, but it's such a big operation
         * that we don't want TravisCI's petty machines to try to run it!
         */
        if (GlobalState.ParallelismConf.PARALLELISM_ALLOWED) {
            double[][] a = TestSuite.getRandom(5, 1500).getDataRef();
            double[][] b = TestSuite.getRandom(1500, 2).getDataRef();
            MatUtils.multiply(a, b); // force block mat
        }
    }

    @Test
    public void testDimCheckIAE1() {
        assertThrows(IllegalArgumentException.class, () -> {
            Array2DRowRealMatrix matrix = new Array2DRowRealMatrix();
            MatUtils.checkDims(matrix);
        });
    }

    @Test
    public void testDimCheckIAE2() {
        assertThrows(IllegalArgumentException.class, () -> {
            double[][] a = new double[5][2];
            double[][] b = new double[5][];
            MatUtils.checkDimsPermitEmpty(a, b);
        });
    }

    @Test
    public void testDimCheckIAE3() {
        assertThrows(IllegalArgumentException.class, () -> {
            boolean[][] a = new boolean[5][2];
            boolean[][] b = new boolean[5][];
            MatUtils.checkDimsPermitEmpty(a, b);
        });
    }

    @Test
    public void testDimCheckIAE4() {
        assertThrows(IllegalArgumentException.class, () -> {
            boolean[][] b = new boolean[5][];
            MatUtils.checkDimsPermitEmpty(b);
        });
    }

    @Test
    public void testDimCheckIAE5() {
        assertThrows(IllegalArgumentException.class, () -> {
            int[][] b = new int[5][];
            MatUtils.checkDimsPermitEmpty(b);
        });
    }

    @Test
    public void testDimCheckIAE6() {
        assertThrows(IllegalArgumentException.class, () -> {
            double[][] b = new double[5][];
            MatUtils.checkDimsPermitEmpty(b);
        });
    }

    @Test
    public void testDimCheckIAE7() {
        assertThrows(IllegalArgumentException.class, () -> {
            boolean[][] b = new boolean[1][];
            MatUtils.checkDimsPermitEmpty(b);
        });
    }

    @Test
    public void testDimCheckIAE8() {
        assertThrows(IllegalArgumentException.class, () -> {
            int[][] b = new int[1][];
            MatUtils.checkDimsPermitEmpty(b);
        });
    }

    @Test
    public void testDimCheckIAE9() {
        assertThrows(IllegalArgumentException.class, () -> {
            double[][] b = new double[1][];
            MatUtils.checkDimsPermitEmpty(b);
        });
    }

    @Test
    public void testNUMEBoolean1() {
        assertThrows(NonUniformMatrixException.class, () -> {
            boolean[][] b = new boolean[][]{
                new boolean[]{true, false},
                new boolean[]{true}
            };
            MatUtils.checkDimsForUniformity(b);
        });
    }

    @Test
    public void testDimCheckDME1() {
        assertThrows(DimensionMismatchException.class, () -> {
            Array2DRowRealMatrix matrix1 = new Array2DRowRealMatrix(new double[5][2], false);
            Array2DRowRealMatrix matrix2 = new Array2DRowRealMatrix(new double[4][2], false);
            MatUtils.checkDims(matrix1, matrix2);
        });
    }

    @Test
    public void testDimCheckDME2() {
        assertThrows(DimensionMismatchException.class, () -> {
            Array2DRowRealMatrix matrix1 = new Array2DRowRealMatrix(new double[5][2], false);
            Array2DRowRealMatrix matrix2 = new Array2DRowRealMatrix(new double[5][3], false);
            MatUtils.checkDims(matrix1, matrix2);
        });
    }

    @Test
    public void testSum() {
        double[][] d = new double[][]{
            new double[]{1, 2, 3},
            new double[]{4, 5, 6}
        };

        assertTrue(MatUtils.sum(d) == 21.0);

        // Test with one empty row (also non-uniform)
        d = new double[][]{
            new double[]{1, 2, 3},
            new double[]{}
        };

        assertTrue(MatUtils.sum(d) == 6.0);

        // Test empty
        d = new double[][]{
            new double[]{},
            new double[]{}
        };

        assertTrue(MatUtils.sum(d) == 0.0);

        d = new double[][]{
        };

        boolean failed = false;
        try {
            MatUtils.sum(d);
        } catch (IllegalArgumentException e) {
            failed = true;
        } finally {
            if (!failed)
                fail();
        }
    }

    @Test
    public void testCumSum() {
        double[][] d = new double[][]{
            new double[]{1, 2, 3},
            new double[]{4, 5, 6}
        };

        assertTrue(VecUtils.equalsExactly(MatUtils.cumSum(d), new double[]{
            1, 3, 6, 10, 15, 21
        }));

        // Test with one empty row (also non-uniform)
        d = new double[][]{
            new double[]{1, 2, 3},
            new double[]{}
        };

        boolean failed = false;
        try {
            MatUtils.cumSum(d);
        } catch (NonUniformMatrixException e) {
            failed = true;
        } finally {
            if (!failed)
                fail();
            else failed = false; // reset
        }

        // Test empty
        d = new double[][]{
            new double[]{},
            new double[]{}
        };

        try {
            MatUtils.cumSum(d);
        } catch (IllegalArgumentException e) {
            failed = true;
        } finally {
            if (!failed)
                fail();
            else failed = false; // reset
        }

        d = new double[][]{
        };

        try {
            MatUtils.cumSum(d);
        } catch (IllegalArgumentException e) {
            failed = true;
        } finally {
            if (!failed)
                fail();
            else failed = false; // reset
        }
    }

    @Test
    public void testSortColsRowsAsc() {
        double[][] a = new double[][]{
            new double[]{3, 2, 1},
            new double[]{1, 4, 2}
        };

        assertTrue(MatUtils.equalsExactly(MatUtils.sortRowsAsc(a),
            new double[][]{
                new double[]{1, 2, 3},
                new double[]{1, 2, 4}
            }));

        assertTrue(MatUtils.equalsExactly(MatUtils.sortColsAsc(a),
            new double[][]{
                new double[]{1, 2, 1},
                new double[]{3, 4, 2}
            }));
    }

    @Test
    public void testNullOK() {
        double[][] d = null;
        int[][] i = null;
        boolean[][] b = null;

        assertNull(MatUtils.copy(d));
        assertNull(MatUtils.copy(i));
        assertNull(MatUtils.copy(b));

        assertTrue(MatUtils.equalsExactly(d, d));
        assertTrue(MatUtils.equalsExactly(i, i));
        assertTrue(MatUtils.equalsExactly(b, b));
    }

    @Test
    public void testSeriesNaNs() {
        final double[][] a = new double[][]{
            new double[]{1, 2, 3},
            new double[]{4, 5, 6}
        };

        final double[][] b = new double[][]{
            new double[]{1, 2, 3},
            new double[]{4, 5, Double.NaN}
        };

        MatSeries ase = new MatSeries(a, Inequality.EQUAL_TO, Double.NaN);
        MatSeries bse = new MatSeries(b, Inequality.EQUAL_TO, Double.NaN);
        MatSeries asn = new MatSeries(a, Inequality.NOT_EQUAL_TO, Double.NaN);
        MatSeries bsn = new MatSeries(b, Inequality.NOT_EQUAL_TO, Double.NaN);

        assertFalse(ase.any());
        assertFalse(ase.all());
        assertTrue(bse.any());
        assertFalse(bse.all());
        assertTrue(asn.all());
        assertFalse(bsn.all());
        assertTrue(asn.any());
        assertTrue(bsn.any());

        final double[][] c = new double[][]{
            new double[]{Double.NaN, Double.NaN},
            new double[]{Double.NaN, Double.NaN}
        };

        MatSeries cse = new MatSeries(c, Inequality.EQUAL_TO, Double.NaN);
        MatSeries csn = new MatSeries(c, Inequality.NOT_EQUAL_TO, Double.NaN);

        assertTrue(cse.all());
        assertTrue(cse.any());
        assertFalse(csn.all());
        assertFalse(csn.any());
    }

    @Test
    public void testVerySmallParallelJob() {
        /*
         * Travis CI is not too capable of extremely large parallel jobs,
         * but we might be able to get away with small ones like this.
         */
        final boolean orig = GlobalState.ParallelismConf.PARALLELISM_ALLOWED;
        try {
            /*
             * No matter the specs of the system testing this, we
             * need to ensure it will be able to force parallelism
             */
            GlobalState.ParallelismConf.PARALLELISM_ALLOWED = true;
            final double[][] A = MatUtils.randomGaussian(5, 3);
            final double[][] B = MatUtils.randomGaussian(3, 2);

            final double[][] C = MatUtils.multiply(A, B);
            assertTrue(MatUtils.equalsExactly(C, MatUtils.multiplyDistributed(A, B)));
        } finally {
            /*
             * Reset
             */
            GlobalState.ParallelismConf.PARALLELISM_ALLOWED = orig;
        }
    }

    @Test
    public void testNPE() {
        boolean a = false;
        try {
            MatUtils.checkDimsPermitEmpty(new double[5][], new double[5][]);
        } catch (IllegalArgumentException i) {
            a = true;
        } finally {
            assertTrue(a);
        }

        a = false;
        try {
            MatUtils.checkDimsPermitEmpty(new int[5][], new int[5][]);
        } catch (IllegalArgumentException i) {
            a = true;
        } finally {
            assertTrue(a);
        }

        a = false;
        try {
            MatUtils.checkDimsPermitEmpty(new boolean[5][], new boolean[5][]);
        } catch (IllegalArgumentException i) {
            a = true;
        } finally {
            assertTrue(a);
        }
    }

    @Test
    public void coverage() {
        String[][] s = null;
        assertNull(MatUtils.copy(s));

        assertFalse(MatUtils.equalsWithTolerance(new double[][]{new double[]{1}}, new double[][]{new double[]{1}, new double[]{2}}));
        assertFalse(MatUtils.equalsExactly(new int[][]{new int[]{1}}, new int[][]{new int[]{1}, new int[]{2}}));
        assertFalse(MatUtils.equalsExactly(new boolean[][]{new boolean[]{false}}, new boolean[][]{new boolean[]{false}, new boolean[]{true}}));

        boolean a = false;
        double[][] d1 = new double[][]{
            new double[]{1, 2, 3}
        };

        double[][] d2 = new double[][]{
            new double[]{1, 2}
        };

        try {
            MatUtils.rbind(d1, d2);
        } catch (DimensionMismatchException d) {
            a = true;
        } finally {
            assertTrue(a);
        }

        /*
         * Test int reshape
         */
        int[][] i = new int[][]{
            new int[]{1, 2, 3},
            new int[]{4, 5, 6}
        };

        assertTrue(MatUtils.equalsExactly(MatUtils.reshape(i, 3, 2), new int[][]{
            new int[]{1, 2},
            new int[]{3, 4},
            new int[]{5, 6}
        }));


        /*
         * Test out of bounds for getColumns
         */
        final int[] cols = new int[]{0, 1, 20};
        final double[][] mat = new double[][]{
            new double[]{1, 2, 3},
            new double[]{1, 2, 3}
        };

        a = false;
        try {
            MatUtils.getColumns(mat, cols);
        } catch (IndexOutOfBoundsException ioob) {
            a = true;
        } finally {
            assertTrue(a);
        }

        assertTrue(new VecUtils.IntSeries(new int[]{1, 2, 3}, Inequality.EQUAL_TO, 1).any());
    }

    @Test
    public void testSliceMatrix() {
        double[][] d = MatUtils.randomGaussian(5, 5);

        assertTrue(MatUtils.slice(d, 0, 3).length == 3);
        boolean a;

        /*
         * Catch AIOOB
         */
        a = false;
        try {
            MatUtils.slice(d, 0, 50);
        } catch (ArrayIndexOutOfBoundsException ai) {
            a = true;
        } finally {
            assertTrue(a);
        }

        a = false;
        try {
            MatUtils.slice(d, -1, 3);
        } catch (ArrayIndexOutOfBoundsException ai) {
            a = true;
        } finally {
            assertTrue(a);
        }

        a = false;
        try {
            MatUtils.slice(d, 6, 7);
        } catch (ArrayIndexOutOfBoundsException ai) {
            a = true;
        } finally {
            assertTrue(a);
        }

        a = false;
        try {
            MatUtils.slice(d, 4, 2);
        } catch (IllegalArgumentException ai) {
            a = true;
        } finally {
            assertTrue(a);
        }

        assertTrue(MatUtils.slice(d, 0, 0).length == 0);
    }
}
