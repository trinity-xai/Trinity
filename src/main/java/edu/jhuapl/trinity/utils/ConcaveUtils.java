package edu.jhuapl.trinity.utils;

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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.StringTokenizer;

/**
 *
 */
public class ConcaveUtils {

    static String dataSetFile = "";                // input
    static String cvxListFile = "";                 // input
    static String ccvDataSetFile = "";              // output
    static String ccvListFile = "";                   // output

    static int NO_OF_DIM = 0;                              // dimension of data
    static double N = 0;                                  // threshold for digging

    double[][] orgData;
    double[] classCenter;

    ArrayList<int[]> cvxList = new ArrayList<int[]>();             //convex list

    ArrayList<double[]> nvList = new ArrayList<double[]>();        //normal vector list

    /*///////////////////////////////////////////////////////////////////////////////////////////
	Calculate Center point of given class(OH)
	///////////////////////////////////////////////////////////////////////////////////////////*/
    public void calcClassCenter() {
        classCenter = new double[NO_OF_DIM];
        double[] tmpRecoed = new double[NO_OF_DIM];

        for (int i = 0; i < orgData.length; i++) {
            for (int j = 0; j < NO_OF_DIM; j++) {
                tmpRecoed[j] += orgData[i][j];
            }
        }

        for (int i = 0; i < NO_OF_DIM; i++) {
            classCenter[i] = tmpRecoed[i] / orgData.length;
        }
    }


    /*///////////////////////////////////////////////////////////////////////////////////////////
	Calculate angle between two vectors (OH)
	///////////////////////////////////////////////////////////////////////////////////////////*/
    public double calcAngle(double[] vectorA, double[] vectorB) {
        double angle = 0;

        double normA = 0, normB = 0, innerProduct = 0;
        for (int i = 0; i < NO_OF_DIM; i++) {
            normA += Math.pow(vectorA[i], 2);
            normB += Math.pow(vectorB[i], 2);
            innerProduct += vectorA[i] * vectorB[i];
        }
        normA = Math.sqrt(normA);
        normB = Math.sqrt(normB);
        angle = innerProduct / (normA * normB);

        return angle;
    }

    /*///////////////////////////////////////////////////////////////////////////////////////////
	Calculate Euclidean distance between point A and B(OH) 20100626
	///////////////////////////////////////////////////////////////////////////////////////////*/
    public double calcEDistance(double[] pointA, double[] pointB) {
        double dist = -1;
        double tmpDistance = 0;
        for (int i = 0; i < NO_OF_DIM; i++) {
            tmpDistance += Math.pow(pointA[i] - pointB[i], 2);
        }

        dist = Math.sqrt(tmpDistance);
        return dist;
    }

    /*///////////////////////////////////////////////////////////////////////////////////////////
	Calculate distance between point A and component B(OH) 20100626
	///////////////////////////////////////////////////////////////////////////////////////////*/
    public double calcPCDistance(double[] pointA, int idxComponentB) {
        double dist = -1;

        int getRecord[] = (int[]) cvxList.get(idxComponentB);

        switch (NO_OF_DIM) {
            case 2:  ////////////////////////////////////////////////////////////////////// 2-Dimensional
                double x0 = pointA[0],
                    y0 = pointA[1];                                     // pa
                double x1 = orgData[getRecord[0] - 1][0],
                    y1 = orgData[getRecord[0] - 1][1];  // sa
                double x2 = orgData[getRecord[1] - 1][0],
                    y2 = orgData[getRecord[1] - 1][1];  // sb

                double a, b, c, t;
                a = Math.pow(x1 - x0, 2) + Math.pow(y1 - y0, 2);          // SQDS(sa,pa);
                b = Math.pow(x2 - x0, 2) + Math.pow(y2 - y0, 2);          // SQDS(sb,pa);
                c = Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2);          // SQDS(sa,sb);

                if (a < b) {
                    double tmp = a;
                    a = b;
                    b = tmp;
                }
                if (a > b + c || c < 0.000000001) {
                    dist = Math.sqrt(b);
                } else {
                    //t=sa.x*sb.y-sb.x*sa.y+sb.x*pa.y-pa.x*sb.y+pa.x*sa.y-sa.x*pa.y;
                    t = x1 * y2 - x2 * y1 + x2 * y0 - x0 * y2 + x0 * y1 - x1 * y0;
                    dist = (double) Math.abs(t) / (double) Math.sqrt(c);
                }
                return dist;
            //break;

            // case 3:   ////////////////////////////////////////////////////////////////////// 3-Dimensional
            // Implement later
            // break;
            default: ////////////////////////////////////////////////////////////////////// n-Dimensional (n>3)

                // find nearest edge point ------------------------------------------------
                int idxNearEdgePoint = -1;
                double minDistance = 999999;

                for (int m = 0; m < NO_OF_DIM; m++) {
                    double tmpDistance = calcEDistance(pointA, orgData[getRecord[m] - 1]);
                    if (tmpDistance < minDistance) {
                        minDistance = tmpDistance;
                        idxNearEdgePoint = getRecord[m];
                    }
                }
                double[] nearEdgePoint = orgData[idxNearEdgePoint - 1];

                // find opposite mid point of nearEdgePoint ------------------------------------------
                double[] oppositeMidPoint = new double[NO_OF_DIM];

                for (int n = 0; n < NO_OF_DIM; n++) {
                    if (getRecord[n] == idxNearEdgePoint) {
                        continue;
                    }
                    for (int m = 0; m < NO_OF_DIM; m++) {
                        oppositeMidPoint[m] += orgData[getRecord[n] - 1][m];
                    }
                }

                for (int m = 0; m < NO_OF_DIM; m++) {
                    oppositeMidPoint[m] /= (NO_OF_DIM - 1);
                }

                // find inside point of Component ------------------------------------------
                double edgeLength = calcEDistance(pointA, oppositeMidPoint);
                double[] minusFactor = new double[NO_OF_DIM];

                for (int m = 0; m < NO_OF_DIM; m++) {
                    minusFactor[m] = ((nearEdgePoint[m] - oppositeMidPoint[m]) / edgeLength) * 0.1;
                }

                double[] insidePoint = new double[NO_OF_DIM];
                for (int m = 0; m < NO_OF_DIM; m++) {
                    insidePoint[m] = nearEdgePoint[m] - minusFactor[m];
                }

                // claculate distance ------------------------------------------
                dist = calcEDistance(pointA, insidePoint);

        }

        return dist;
    }

    /*///////////////////////////////////////////////////////////////////////////////////////////
	Count instances of dataset.
	///////////////////////////////////////////////////////////////////////////////////////////*/
    public int countInstance(String targetFile) {
        int cnt = 0;
        String s1;
        try {
            BufferedReader in = new BufferedReader(new FileReader(targetFile));
            int i = 0;
            while ((s1 = in.readLine()) != null) {
                cnt++;
            }

            in.close();
        } catch (IOException IOe) {
            System.err.println(IOe);
            System.exit(1);
        }
        return cnt;
    }

    /*////////////////////////////////////////////////////////////////////////////////////////////
	Load original data from data file
	////////////////////////////////////////////////////////////////////////////////////////////*/
    public void loadOriginalData() {
        // Original data
        orgData = new double[countInstance(dataSetFile)][NO_OF_DIM + 1];    // +1, to add flag field

        String s1;
        try {
            BufferedReader in = new BufferedReader(new FileReader(dataSetFile));
            int i = 0;
            while ((s1 = in.readLine()) != null) {
                StringTokenizer token = new StringTokenizer(s1, ",");
                for (int j = 0; j < NO_OF_DIM; j++) {
                    orgData[i][j] = Double.parseDouble(token.nextToken().trim());
                }
                i++;
            }
            in.close();

        } catch (IOException IOe) {
            System.err.println(IOe);
            System.exit(1);
        }
    }

    /*////////////////////////////////////////////////////////////////////////////////////////////
	Load original data from data file
	////////////////////////////////////////////////////////////////////////////////////////////*/
    public int load_CountFeature() {
        // Original data
        orgData = new double[countInstance(dataSetFile)][NO_OF_DIM + 1];    // +1, to add flag field

        String s1, s2;
        int count = 0;
        try {
            BufferedReader in = new BufferedReader(new FileReader(dataSetFile));
            if ((s1 = in.readLine()) != null) {

                StringTokenizer token = new StringTokenizer(s1, ",");

                while (token.hasMoreElements()) {
                    token.nextToken();
                    count++;
                }
            }
            in.close();
        } catch (IOException IOe) {
            System.err.println(IOe);
            System.exit(1);
        }
        return count;
    }

    /*////////////////////////////////////////////////////////////////////////////////////////////
	Load convex list from file.
	////////////////////////////////////////////////////////////////////////////////////////////*/
    public void loadConvexList() {
        // Read cvxList file ---------------------------------------------------------------
        String s1;
        try {
            BufferedReader in = new BufferedReader(new FileReader("tmp/" + cvxListFile));
            while ((s1 = in.readLine()) != null) {
                StringTokenizer token = new StringTokenizer(s1, ",");
                int tmpRecord[] = new int[NO_OF_DIM];
                for (int j = 0; j < NO_OF_DIM; j++) {
                    tmpRecord[j] = Integer.parseInt(token.nextToken().trim());
                }
                cvxList.add(tmpRecord);
            }
            in.close();
        } catch (IOException IOe) {
            System.err.println(IOe);
            System.exit(1);
        }
    }

    /*////////////////////////////////////////////////////////////////////////////////////////////
	Turn on the flag of vertex via convex list.
	////////////////////////////////////////////////////////////////////////////////////////////*/
    public void flagVertexPoints() {
        int getRecord[];
        for (int i = 0; i < cvxList.size(); i++) {
            getRecord = cvxList.get(i);
            for (int j = 0; j < getRecord.length; j++) {
                orgData[getRecord[j] - 1][NO_OF_DIM] = 1;    // set flag field to 1
            }
        }
    }


    /*////////////////////////////////////////////////////////////////////////////////////////////
	Convert format of original data.
	qhull(qconvex.exe) dose not use comma for delimiter.
	////////////////////////////////////////////////////////////////////////////////////////////*/
    public void convertFormat_Origin() {
        // Read file (using comma) ---------------------------------------------------------------
        String s1;
        double tmpRecord[][] = null;
        int count = 0;
        int i = 0;
        try {
            BufferedReader in = new BufferedReader(new FileReader(dataSetFile));
            count = countInstance(dataSetFile);
            tmpRecord = new double[count][NO_OF_DIM];
            while ((s1 = in.readLine()) != null) {
                StringTokenizer token = new StringTokenizer(s1.trim(), ","); // delimiter is comma
                for (int j = 0; j < NO_OF_DIM; j++) {
                    tmpRecord[i][j] = Double.parseDouble(token.nextToken().trim());
                }
                i++;
            }
            in.close();
        } catch (IOException IOe) {
            System.err.println(IOe);
            System.exit(1);
        }

        // Save converted file (using space)
        try {
            FileWriter fw_1 = new FileWriter("tmp/" + dataSetFile + "_converted");
            BufferedWriter bw_1 = new BufferedWriter(fw_1);
            PrintWriter outFile_1 = new PrintWriter(bw_1);
            outFile_1.print(NO_OF_DIM);
            outFile_1.println("");
            outFile_1.print(count);
            outFile_1.println("");
            for (i = 0; i < count; i++) {
                for (int j = 0; j < NO_OF_DIM; j++) {
                    outFile_1.print(tmpRecord[i][j] + "  ");  // delimiter is space
                }
                outFile_1.println("");
            }
            outFile_1.close();
        } catch (IOException IOe) {
            System.err.println(IOe);
            System.exit(1);
        }
    }

    /*////////////////////////////////////////////////////////////////////////////////////////////
	Convert format of result
	Results of qhull(qconvex.exe) use space for delimiter.
	////////////////////////////////////////////////////////////////////////////////////////////*/
    public void convertFormat_Result() {
        // Read result file ---------------------------------------------------------------
        String s1;
        int tmpRecord[][] = null;
        int count = 0;
        int i = 0;
        try {
            BufferedReader in = new BufferedReader(new FileReader("tmp/result_"));
            count = Integer.parseInt(in.readLine().trim());
            tmpRecord = new int[count][NO_OF_DIM];
            while ((s1 = in.readLine()) != null) {
                StringTokenizer token = new StringTokenizer(s1.trim(), " "); // delimiter is space
                for (int j = 0; j < NO_OF_DIM; j++) {
                    tmpRecord[i][j] = Integer.parseInt(token.nextToken().trim());
                }
                i++;
            }
            in.close();
        } catch (IOException IOe) {
            System.err.println(IOe);
            System.exit(1);
        }

        // Save converted result
        try {
            FileWriter fw_1 = new FileWriter("tmp/Result");
            BufferedWriter bw_1 = new BufferedWriter(fw_1);
            PrintWriter outFile_1 = new PrintWriter(bw_1);
            for (i = 0; i < count; i++) {
                for (int j = 0; j < NO_OF_DIM; j++) {
                    // delimiter is comma AND INDEX VALUE START AT 1. therefore plus 1 to tmpRecord value.
                    outFile_1.print((tmpRecord[i][j] + 1) + ", ");
                }
                outFile_1.println("");
            }
            outFile_1.close();
        } catch (IOException IOe) {
            System.err.println(IOe);
            System.exit(1);
        }

    }

    /*////////////////////////////////////////////////////////////////////////////////////////////
	Make convex hull list through qhull program.
	Result is saved in 'tmp' diriectory.
	////////////////////////////////////////////////////////////////////////////////////////////*/
    public void makeConvexList() {
        Runtime oRuntime = Runtime.getRuntime();
        Process oProcess;
        try { // Call qconvex program with options.
            oProcess = oRuntime.exec("cmd /c qconvex.exe i Qt TI " + "tmp/" + dataSetFile + "_converted" + " TO tmp/result_");
            BufferedReader stdOut = new BufferedReader(new InputStreamReader(oProcess.getInputStream()));
            String s = "";
            while ((s = stdOut.readLine()) != null) {
                System.out.println(s);
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /*////////////////////////////////////////////////////////////////////////////////////////////
	Concave Hull Algorithm
	Dig the boundary as a result of comparing between nearest inner point and decision distance.
	////////////////////////////////////////////////////////////////////////////////////////////*/
    public void findConcave() {
        System.out.println("Work is start --------------- " + cvxList.size());
        int count_array_change = 0;
        for (int i = 0; i < cvxList.size(); i++) {
            // Step. 0 Calculate average of edges of each component
            double avgEdgeLength = 0;
            int cCnt = 0;
            int getRecord[] = (int[]) cvxList.get(i);

            int[] indices;
            CombinationGenerator cg = new CombinationGenerator(getRecord.length, 2);
            while (cg.hasMore()) {
                indices = cg.getNext();

                int x = getRecord[indices[0]];
                int y = getRecord[indices[1]];

                double tmpLength = calcEDistance(orgData[x - 1], orgData[y - 1]);
                avgEdgeLength += tmpLength;
                cCnt++;
            }

            avgEdgeLength /= (double) cCnt;
            // End of Step. 0

            // Step. 1 Find nearest inner point of each component
            ArrayList<Double> arrLength = new ArrayList<Double>();     // Distances of inner points
            ArrayList<Integer> arrIdx = new ArrayList<Integer>();   // Indexes of inner points

            // Calculate average length between every points and edges of each component
            for (int j = 0; j < orgData.length; j++) {
                //if point is already included in vertex list, skip the point.
                if (orgData[j][NO_OF_DIM] == 1.0) {
                    continue;
                }

                double tmpAverage = 0;

                for (int x = 0; x < NO_OF_DIM; x++) {
                    double tmpLength = calcEDistance(orgData[j], orgData[getRecord[x] - 1]);
                    tmpAverage += tmpLength;
                }
                tmpAverage /= (double) NO_OF_DIM;

                arrLength.add(tmpAverage);
                arrIdx.add(j);
            }

            if (arrLength.isEmpty()) {
                break;
            }

            // Find nearest inner point
            double shortestLength = 999999;
            int nearestPoint = 0;
            for (int k = 0; k < arrLength.size(); k++) {
                double currLength = (double) arrLength.get(k);
                if (currLength < shortestLength) {
                    // check if current point is close to neighbor compoinnt OH 20110111
                    {   ////////////////////////////////////////////////////////////////////////////

                        int idxNearEdgePoint = -1;
                        double minDistance = 999999;

                        for (int m = 0; m < NO_OF_DIM; m++) {
                            double tmpDistance = calcEDistance(orgData[(int) arrIdx.get(k)], orgData[getRecord[m] - 1]);
                            if (tmpDistance < minDistance) {
                                minDistance = tmpDistance;
                                idxNearEdgePoint = getRecord[m];
                            }
                        }

                        // point is nearest to given component
                        // collect components that link to given component
                        ArrayList<String> linkComponent = new ArrayList<String>();

                        for (int n = 0; n < cvxList.size(); n++) {
                            if (cvxList.get(n) == null || n == i) {
                                continue;
                            }

                            int[] cvxRecord = cvxList.get(n);

                            for (int m = 0; m < NO_OF_DIM; m++) {
                                if (cvxRecord[m] == (idxNearEdgePoint)) {
                                    linkComponent.add(String.valueOf(n));
                                    break;
                                }
                            }
                        }

                        int closerToNeighbor = 0;
                        for (int r = 0; r < linkComponent.size(); r++) {
                            int idxLinkComponent = Integer.parseInt(linkComponent.get(r));
                            if (calcPCDistance(orgData[(int) arrIdx.get(k)], idxLinkComponent) < calcPCDistance(orgData[(int) arrIdx.get(k)], i)) {
                                closerToNeighbor = 1;
                            }
                        }

                        if (closerToNeighbor == 0) {
                            nearestPoint = (int) arrIdx.get(k);
                            shortestLength = currLength;
                        }
                    } ////////////////////////////////////////////////////////////////////////////

                }

            }

            // step. 2 Calculate shortest length between each point of component and nearest inner point.
            // This distance is called decision distance.
            double minLength = 99999;
            int closestPoint = 99999;
            for (int x = 0; x < NO_OF_DIM; x++) {
                double tmpLength = 0;
                for (int k = 0; k < NO_OF_DIM; k++) {
                    tmpLength += Math.pow(orgData[nearestPoint][k] - orgData[getRecord[x] - 1][k], 2);
                }
                tmpLength = Math.sqrt(tmpLength);
                if (tmpLength < minLength) {
                    minLength = tmpLength;
                    closestPoint = getRecord[x];
                }
            }

            // step. 3 Compare the ratio of decision distance with threshold N.
            // if the ratio bigger than N, nearest inner point is inserted to concave list.
            if (minLength > 0 && (double) avgEdgeLength / (double) minLength > N) {
                // 3.1 vertex flag on.
                orgData[nearestPoint][NO_OF_DIM] = 1;
//				cvxList.set(i, null) ;                                      // nullify

                // 3.2 make new components through combination of a selected component and nearest inner point.
                int tmpIdx = 0;

                cg = new CombinationGenerator(NO_OF_DIM, NO_OF_DIM - 1);
                while (cg.hasMore()) {
                    indices = cg.getNext();
                    int newRecord[] = new int[NO_OF_DIM];

                    for (int q = 0; q < NO_OF_DIM - 1; q++) {
                        newRecord[q] = getRecord[indices[q]];
                    }

                    newRecord[NO_OF_DIM - 1] = nearestPoint + 1;   //
                    System.out.println("newRecord = " + newRecord[0] + "," + newRecord[1]);
                    cvxList.add(newRecord);    //add new components

                }
                cvxList.set(i, null);                                      // nullify
                count_array_change++;
            }
        }

        // Delete Nullified component
        for (int k = cvxList.size() - 1; k >= 0; k--) {
            if (cvxList.get(k) == null) {
                cvxList.remove(k);
                System.out.println("removed :" + k);
            }
        }

        System.out.println("Work is finished --------------- " + cvxList.size());

    }

    /*////////////////////////////////////////////////////////////////////////////////////////////
	Save concave hull list data and flagged original data
	////////////////////////////////////////////////////////////////////////////////////////////*/
    public void SaveRseult() {
        // Save concave-hull data
        try {
            FileWriter fw_1 = new FileWriter(ccvListFile);
            BufferedWriter bw_1 = new BufferedWriter(fw_1);
            PrintWriter outFile_1 = new PrintWriter(bw_1);

            for (int i = 0; i < cvxList.size(); i++) {
                int currRecord[] = (int[]) cvxList.get(i);
                for (int j = 0; j < currRecord.length; j++) {
                    outFile_1.print(currRecord[j] + ", ");
                }
                outFile_1.println("");
            }

            outFile_1.close();

        } catch (IOException IOe) {
            System.err.println(IOe);
            System.exit(1);
        }
    }

    /*////////////////////////////////////////////////////////////////////////////////////////////
	Main
	////////////////////////////////////////////////////////////////////////////////////////////*/
    public static void main(String[] args) {
        ConcaveUtils obj = new ConcaveUtils();
        //count feature number


        // parsing and setting options
        if (args[0].equals("-p")) {
            dataSetFile = args[1];
            cvxListFile = "result";
            ccvDataSetFile = args[2];
            ccvListFile = ccvDataSetFile + "_ccvList";
            NO_OF_DIM = obj.load_CountFeature();
            if (args.length == 4) {
                N = Double.parseDouble(args[3]);
            } else {
                N = 2;
            }
        } else if (args[0].equals("-f")) {
            cvxListFile = args[1];
            dataSetFile = args[2];
            ccvDataSetFile = args[3];
            ccvListFile = ccvDataSetFile + "_ccvList";
            NO_OF_DIM = obj.load_CountFeature();
            if (args.length == 5) {
                N = Double.parseDouble(args[4]);
            } else {
                N = 2;
            }
        }

        // make /tmp directory if there is no it
        File tmpDir = new File("tmp");
        if (!tmpDir.exists() || !tmpDir.isDirectory()) {
            tmpDir.mkdir();
        }

        obj.convertFormat_Origin();
        obj.makeConvexList(); // call qconvex program.
        obj.convertFormat_Result();

        obj.loadConvexList();
        obj.loadOriginalData();

        obj.flagVertexPoints(); // flag convex list

        obj.calcClassCenter();

        obj.findConcave();
        obj.SaveRseult();
    }
}
