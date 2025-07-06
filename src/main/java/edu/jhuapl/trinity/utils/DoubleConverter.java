package edu.jhuapl.trinity.utils;

import javafx.util.StringConverter;

import java.text.DecimalFormat;
import java.text.ParseException;

/**
 * @author Sean Phillips
 */
public class DoubleConverter extends StringConverter<Double> {
    private DecimalFormat df; // = new DecimalFormat("###.###");

    public DoubleConverter(String pattern) {
        df = new DecimalFormat(pattern);
    }

    @Override
    public String toString(Double object) {
        if (object == null) {
            return "";
        }
        return df.format(object);
    }

    @Override
    public Double fromString(String string) {
        try {
            if (string == null) {
                return null;
            }
            string = string.trim();
            if (string.length() < 1) {
                return null;
            }
            return df.parse(string).doubleValue();
        } catch (ParseException ex) {
            throw new RuntimeException(ex);
        }
    }

    ;
}
