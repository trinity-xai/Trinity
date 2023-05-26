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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Sean Phillips
 * Simple POJO class intended to allow configuration to be set from file
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Configuration {

    public static String DEFAULT_PROPERTIES_FILENAME = "config_props.properties";
    public static String BUILD_PROPERTIES_FILENAME = "/edu/jhuapl/trinity/build.properties";
    public Properties configProps;

    public Configuration() {

    }

    public Configuration(String filename) throws FileNotFoundException, IOException {
        Properties madProps = new Properties();
        madProps.load(new FileInputStream(filename));
        configProps = madProps;
    }

    public static Properties getBuildProps() {
        Properties madProps = new Properties();
        try (InputStream is = Configuration.class.getResourceAsStream(BUILD_PROPERTIES_FILENAME)) {
            madProps.load(is);
        } catch (IOException ex) {
            Logger.getLogger(Configuration.class.getName()).log(Level.SEVERE, null, ex);
        }
        return madProps;
    }

    public static String getBuildDate() {
        Properties madProps = new Properties();
        try (InputStream is = Configuration.class.getResourceAsStream(BUILD_PROPERTIES_FILENAME)) {
            madProps.load(is);
        } catch (IOException ex) {
            Logger.getLogger(Configuration.class.getName()).log(Level.SEVERE, null, ex);
        }
        return (String) madProps.get("buildDate");
    }

    public static Configuration defaultConfiguration() {
        Configuration newConfig = new Configuration();
        Properties madProps = new Properties();
        try (InputStream is = Configuration.class.getResourceAsStream(DEFAULT_PROPERTIES_FILENAME)) {
            madProps.load(is);
        } catch (IOException ex) {
            Logger.getLogger(Configuration.class.getName()).log(Level.SEVERE, null, ex);
        }
        newConfig.configProps = madProps;
        return newConfig;
    }

}
