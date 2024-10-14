/* Copyright (C) 2021 - 2023 The Johns Hopkins University Applied Physics Laboratory LLC */

package edu.jhuapl.trinity.utils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * @author Sean Phillips
 * Simple POJO class intended to allow configuration to be set from file
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Configuration {
    private static final Logger LOG = LoggerFactory.getLogger(Configuration.class);
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
            LOG.error(null, ex);
        }
        return madProps;
    }

    public static String getBuildDate() {
        Properties madProps = new Properties();
        try (InputStream is = Configuration.class.getResourceAsStream(BUILD_PROPERTIES_FILENAME)) {
            madProps.load(is);
        } catch (IOException ex) {
            LOG.error(null, ex);
        }
        return (String) madProps.get("timestamp");
    }

    public static Configuration defaultConfiguration() {
        Configuration newConfig = new Configuration();
        Properties madProps = new Properties();
        try (InputStream is = Configuration.class.getResourceAsStream(DEFAULT_PROPERTIES_FILENAME)) {
            madProps.load(is);
        } catch (IOException ex) {
            LOG.error(null, ex);
        }
        newConfig.configProps = madProps;
        return newConfig;
    }

}
