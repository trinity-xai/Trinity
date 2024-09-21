package edu.jhuapl.trinity.utils.marchingcubes;

/*-
 * #%L
 * trinity
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class Main {
    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    private static String usage = "This script may be executed in either benchmark or extract mode. Mode is specified by the first parameter [benchmark, extract].\nParameters: \n\t-input-vol\t Specifies path to the input volume. If this parameter is set volume dimensions(-vol-dim), data type(-data-type) and iso value(-iso) must also be given.\n\t-vol-dim\t Specifies the generated/read volume dimensions. Dimensions should be given as unsigned integers in format; -vol-dim X Y Z.\n\t-data-type\t Specifies the input file or generated data type. Options [char, uchar, short, ushort, int, uint, float, double].\n\t-vox-dim\t Specifies voxel dimensions used in mesh construction. Dimensions should be given as floating point numbers in format: -vox-dim X Y Z.\n\t-nThread\t Number of threads used in Marching cubes algorithm.This parameter can be either given as a single unsigned integer value or two unsigned integer values in benchmark mode, specifying the range of thread executions that will be tested.\n\t-iter\t\t Used only in benchmark mode to determine how many iterations should be executed for each configuration.\n\t-iso\t\t Isovalue that is used as a threshold for determining active voxels. Type should match the data type.\n\t-o\t\t Path to output file. In extract mode the mesh is written to file in .obj format [required]. In benchmark mode the results are written to file.\n";
    ;

    private static boolean isUint(String input) {
        try {
            return (Integer.parseInt(input) >= 0);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static boolean isFloat(String input) {
        try {
            Float.parseFloat(input);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static void main(String[] args) {

        // Benchmark or extract mode
        boolean benchmark = false;

        if (args.length < 1) {
            LOG.info(usage);
//            return;
            LOG.info("\n\nAssuming Benchmark mode and generated volume...");
            benchmark = true;
        } else if (args[0].equals("-help")) {
            LOG.info(usage);
        }
        // Read execution type
        else if (args[0].equals("benchmark")) {
            benchmark = true;
        } else if (!args[0].equals("extract")) {
            LOG.info("Invalid execution type. Valid options [extract, benchmark]");
            return;
        }

        // Default num of threads is max available
        int nThreadsMin = java.lang.Thread.activeCount();
        if (nThreadsMin == 0) {
            nThreadsMin = 1;
        }
        int nThreadsMax = nThreadsMin;

        File inputFile = null;
        File outFile = null;
        String type = "double";
        String isoValueStr = null;
        int iterations = 10;    // Default 10 iterations per benchmark

        boolean customSizeSpecified = false;
        int[] size = {64, 64, 64};
        float[] voxSize = {1.0f, 1.0f, 1.0f};


        // Flag parsing
        for (int i = 1; i < args.length; i++) {
            if (args[i].equals("-input-vol")) {
                // Volume path specified
                // Output file path is specified
                if (i + 1 >= args.length || args[i + 1].charAt(0) == '-') {
                    LOG.info("Missing file path after -input-vol flag.");
                    return;
                }

                // Store the file name and offset iterator
                inputFile = new File(args[++i]);

                if (!inputFile.exists() || inputFile.isDirectory()) {
                    LOG.info("Specified volume file does not exist.");
                    return;
                }
            } else if (args[i].equals("-vol-dim")) {
                // Volume dimensions are given
                if (i + 3 >= args.length || args[i + 1].charAt(0) == '-' || args[i + 2].charAt(0) == '-' || args[i + 3].charAt(0) == '-') {
                    LOG.info("Missing volume dimensions after -vol-dim flag.");
                    return;
                }

                String x = (args[++i]);
                String y = (args[++i]);
                String z = (args[++i]);

                if (!isUint(x) || !isUint(y) || !isUint(z)) {
                    LOG.info("Invalid volume dimensions format. Specify dimensions as three unsigned integers.");
                    return;
                }

                customSizeSpecified = true;
                size[0] = Integer.parseInt(x);
                size[1] = Integer.parseInt(y);
                size[2] = Integer.parseInt(z);
            } else if (args[i].equals("-vox-dim")) {
                // Voxel dimensions are given
                if (i + 3 >= args.length) {
                    LOG.info("Missing voxel dimensions after -vox-dim flag.");
                    return;
                }

                String x = args[++i];
                String y = args[++i];
                String z = args[++i];

                if (!isFloat(x) || !isFloat(y) || !isFloat(z)) {
                    LOG.info("Invalid voxel dimensions format. Specify voxel dimensions as three positive floats.");
                    return;
                }

                voxSize[0] = Float.parseFloat(x);
                voxSize[0] = Float.parseFloat(y);
                voxSize[0] = Float.parseFloat(z);
            } else if (args[i].equals("-nThread")) {
                // Number of threads is given
                // FIRST VALUE
                if (i + 1 >= args.length || args[i + 1].charAt(0) == '-') {
                    LOG.info("Missing number or range of threads after -nThread flag.");
                    return;
                }

                // Validate first number
                String tmp = args[++i];

                if (!isUint(tmp)) {
                    LOG.info("Invalid nThread value format. Specify unsigned integer value or two if range.");
                    return;
                }

                // Parse C-str
                nThreadsMin = Integer.parseInt(tmp);

                // SECOND VALUE (If given)
                if (i + 1 < args.length && args[i + 1].charAt(0) != '-') {
                    // Validate second number
                    tmp = args[++i];
                    if (!isUint(tmp)) {
                        LOG.info("Invalid nThread value format. Specify unsigned integer value or two if range.");
                        return;
                    }

                    // Parse C-str
                    nThreadsMax = Integer.parseInt(tmp);
                } else {
                    nThreadsMax = nThreadsMin;
                }

            } else if (args[i].equals("-iso")) {
                // ISO value is given
                if (i + 1 >= args.length) {
                    LOG.info("Missing iso value after -iso flag.");
                    return;
                }

                isoValueStr = args[++i];

                if (!isFloat(isoValueStr)) {
                    LOG.info("Invalid iso value format. Please specify float.");
                    return;
                }
            } else if (args[i].equals("-iter")) {
                // ISO value is given
                if (i + 1 >= args.length) {
                    LOG.info("Missing number of iterations after -iter flag.");
                    return;
                }

                String iterationsStr = args[++i];

                if (!isUint(iterationsStr)) {
                    LOG.info("Invalid iterations value format. Please specify unsigned integer.");
                    return;
                }

                iterations = Integer.parseInt(iterationsStr);
            } else if (args[i].equals("-o")) {
                // Output file path is specified
                if (i + 1 >= args.length || args[i + 1].charAt(0) == '-') {
                    LOG.info("Missing file path after -o flag.");
                    return;
                }

                // Store the file name and offset iterator
                outFile = new File(args[++i]);

                if (outFile.getParentFile() != null && !outFile.getParentFile().exists()) {
                    LOG.info("Specified output file path is invaild.");
                }
            } else if (args[i].equals("-data-type")) {
                // Volume data type is specified
                if (i + 1 >= args.length || args[i + 1].charAt(0) == '-') {
                    LOG.info("Missing type after -data-type flag.");
                    return;
                }

                // Data type is specified (char, uchar, short, ushort, int, uint, float, double)
                if (!args[i + 1].equals("char") && !args[i + 1].equals("uchar") && !args[i + 1].equals("short") && !args[i + 1].equals("ushort") && args[i + 1].equals("uint") && args[i + 1].equals("float") && args[i + 1].equals("double")) {
                    LOG.info("Invalid data type. Available data types: char, uchar, short, ushort, int, uint, float, double.");
                    return;
                }

                type = args[++i];
            } else {
                LOG.info("Unknown parameter: {}", args[i]);
                return;
            }
        }

        if (inputFile != null && (!customSizeSpecified || type == null || isoValueStr == null)) {
            LOG.info("If custom volume is imported, you must input volume dimensions(-vol-dim), data type (-data-type) and iso value (-iso).");
            return;
        }
        //endregion

        if (benchmark) {
            switch (type) {
                case "char" ->
                    BenchmarkHandler.benchmarkChar(inputFile, outFile, size, voxSize, (char) ((isoValueStr != null) ? Integer.parseInt(isoValueStr) : 0.5), nThreadsMin, nThreadsMax, iterations);
                case "uchar" ->
                    BenchmarkHandler.benchmarkChar(inputFile, outFile, size, voxSize, (char) ((isoValueStr != null) ? Integer.parseInt(isoValueStr) : 0.5), nThreadsMin, nThreadsMax, iterations);
                case "short" ->
                    BenchmarkHandler.benchmarkShort(inputFile, outFile, size, voxSize, (short) ((isoValueStr != null) ? Integer.parseInt(isoValueStr) : 0.5), nThreadsMin, nThreadsMax, iterations);
                case "ushort" ->
                    BenchmarkHandler.benchmarkShort(inputFile, outFile, size, voxSize, (short) ((isoValueStr != null) ? Integer.parseInt(isoValueStr) : 0.5), nThreadsMin, nThreadsMax, iterations);
                case "int" ->
                    BenchmarkHandler.benchmarkInt(inputFile, outFile, size, voxSize, ((isoValueStr != null) ? Integer.parseInt(isoValueStr) : 0), nThreadsMin, nThreadsMax, iterations);
                case "uint" ->
                    BenchmarkHandler.benchmarkInt(inputFile, outFile, size, voxSize, ((isoValueStr != null) ? Integer.parseInt(isoValueStr) : 0), nThreadsMin, nThreadsMax, iterations);
                case "float" ->
                    BenchmarkHandler.benchmarkFloat(inputFile, outFile, size, voxSize, ((isoValueStr != null) ? Float.parseFloat(isoValueStr) : 0.5f), nThreadsMin, nThreadsMax, iterations);
                case "double" ->
                    BenchmarkHandler.benchmarkDouble(inputFile, outFile, size, voxSize, ((isoValueStr != null) ? Double.parseDouble(isoValueStr) : 0.5), nThreadsMin, nThreadsMax, iterations);
            }
        } else {
            if (outFile == null) {
                LOG.info("To extract the data the output file path is needed (-o).");
                return;
            }

            switch (type) {
                case "char":
                    ExtractHandler.extractHandlerChar(inputFile, outFile, size, voxSize, (char) ((isoValueStr != null) ? Integer.parseInt(isoValueStr) : 0.5), nThreadsMax);
                    break;
                case "uchar":
                    ExtractHandler.extractHandlerChar(inputFile, outFile, size, voxSize, (char) ((isoValueStr != null) ? Integer.parseInt(isoValueStr) : 0.5), nThreadsMax);
                    break;
                case "short":
                    ExtractHandler.extractHandlerShort(inputFile, outFile, size, voxSize, (short) ((isoValueStr != null) ? Integer.parseInt(isoValueStr) : 0.5), nThreadsMax);
                    break;
                case "ushort":
                    ExtractHandler.extractHandlerShort(inputFile, outFile, size, voxSize, (short) ((isoValueStr != null) ? Integer.parseInt(isoValueStr) : 0.5), nThreadsMax);
                    break;
                case "int":
                    ExtractHandler.extractHandlerInt(inputFile, outFile, size, voxSize, ((isoValueStr != null) ? Integer.parseInt(isoValueStr) : 0), nThreadsMax);
                    break;
                case "uint":
                    ExtractHandler.extractHandlerInt(inputFile, outFile, size, voxSize, ((isoValueStr != null) ? Integer.parseInt(isoValueStr) : 0), nThreadsMax);
                    break;
                case "float":
                    ExtractHandler.extractHandlerFloat(inputFile, outFile, size, voxSize, ((isoValueStr != null) ? Float.parseFloat(isoValueStr) : 0.5f), nThreadsMax);
                    break;
                case "double":
                    ExtractHandler.extractHandlerDouble(inputFile, outFile, size, voxSize, ((isoValueStr != null) ? Double.parseDouble(isoValueStr) : 0.5), nThreadsMax);
                    break;
            }
        }
    }
}
