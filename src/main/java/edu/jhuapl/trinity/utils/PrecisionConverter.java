package edu.jhuapl.trinity.utils;

/*-
 * #%L
 * trinity-1.0.0-SNAPSHOT
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

import javafx.util.StringConverter;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Sean Phillips
 */
public class PrecisionConverter extends StringConverter<Double> {
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
            Logger.getLogger(PrecisionConverter.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }
}
