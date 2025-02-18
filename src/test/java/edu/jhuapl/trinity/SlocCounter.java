/* Copyright (C) 2021 - 2023 The Johns Hopkins University Applied Physics Laboratory LLC */

package edu.jhuapl.trinity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

/**
 * @author Sean Phillips
 */
public class SlocCounter {
    private static final Logger LOG = LoggerFactory.getLogger(SlocCounter.class);

    public static void main(String[] args) throws FileNotFoundException {

        final String folderPath = "C:\\dev\\trinity\\trinity\\src";

        long totalLineCount = 0;
        final List<File> folderList = new LinkedList<>();
        folderList.add(new File(folderPath));
        while (!folderList.isEmpty()) {
            final File folder = folderList.remove(0);
            if (folder.isDirectory() && folder.exists()) {
                LOG.info("Scanning {}", folder.getName());
                final File[] fileList = folder.listFiles();
                for (final File file : fileList) {
                    if (file.isDirectory()) {
                        folderList.add(file);
                    } else if (file.getName().endsWith(".java")
                        || file.getName().endsWith(".fxml")
                        || file.getName().endsWith(".css")) {
                        long lineCount = 0;
                        final Scanner scanner = new Scanner(file);
                        while (scanner.hasNextLine()) {
                            scanner.nextLine();
                            lineCount++;
                        }
                        totalLineCount += lineCount;
                        final String lineCountString;
                        if (lineCount > 99999) {
                            lineCountString = "" + lineCount;
                        } else {
                            final String temp = ("     " + lineCount);
                            lineCountString = temp.substring(temp.length() - 5);
                        }
                        LOG.info("{} lines in {}", lineCountString, file.getName());
                    }
                }
            }
        }
        LOG.info("Scan Complete: {} lines total", totalLineCount);
    }
}
