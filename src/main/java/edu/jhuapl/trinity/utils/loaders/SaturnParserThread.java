package edu.jhuapl.trinity.utils.loaders;

import edu.jhuapl.trinity.data.SaturnShot;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Sean Phillips
 */
public class SaturnParserThread extends Thread {

    List<SaturnShot> measurements;
    List<String> lines;

    public SaturnParserThread(List<String> lines) {
        this.lines = lines;
        measurements = new ArrayList<>(lines.size());
    }

    @Override
    public void run() {
        measurements = lines.stream()
            .map(SaturnShot.csvToSaturnShot)
            .collect(Collectors.toCollection(ArrayList::new));
    }

    public List<SaturnShot> getMeasurements() {
        return measurements;
    }
}
