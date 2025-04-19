package edu.jhuapl.trinity.utils;

import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.NumberFormat;
import java.text.ParseException;

/**
 * @author Sean Phillips
 */
public class PrecisionConverter extends StringConverter<Double> {
    private static final Logger LOG = LoggerFactory.getLogger(PrecisionConverter.class);
    int decimalPlaces = 7;

    public PrecisionConverter(int decimalPlaces) {
        this.decimalPlaces = decimalPlaces;
        nf.setMaximumFractionDigits(decimalPlaces);
        nf.setMinimumFractionDigits(decimalPlaces);
        nf.setGroupingUsed(false);
    }

    private NumberFormat nf = NumberFormat.getNumberInstance();

    {
        nf.setGroupingUsed(false);
        nf.setMaximumFractionDigits(decimalPlaces);
        nf.setMinimumFractionDigits(decimalPlaces);
    }

    @Override
    public String toString(final Double value) {
        try {
            return nf.format(value);
        } catch (IllegalArgumentException e) {
            return "NULL";
        }
    }

    @Override
    public Double fromString(final String s) {
        try {
            return nf.parse(s).doubleValue();
        } catch (ParseException ex) {
            LOG.error(null, ex);
            return null;
        }
    }
}
