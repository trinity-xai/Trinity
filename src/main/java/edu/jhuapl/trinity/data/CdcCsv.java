package edu.jhuapl.trinity.data;

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

import java.util.function.Function;

/**
 * @author Sean Phillips
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CdcCsv {
    //header:
    //county,county_fips,state,county_population,health_service_area_number,
    //health_service_area,health_service_area_population,covid_inpatient_bed_utilization,
    //covid_hospital_admissions_per_100k,covid_cases_per_100k,covid-19_community_level,date_updated
    //Example:
    //American Samoa,60000,American Samoa,47392,901,American Samoa,47392,0.0,2.1,156.14,Low,2022-03-03

    private String county;
    private int county_fips;
    private String state;
    private int county_population;
    private int health_service_area_number;
    private String health_service_area;
    private int health_service_area_population;
    private double covid_inpatient_bed_utilization;
    private double covid_hospital_admissions_per_100k;
    private double covid_cases_per_100k;
    private String covid19_community_level; //"covid-19_community_level"
    private String date_updated;

    public CdcCsv() {

    }

    public static Function<String, CdcCsv> csvToCdcCsv = s -> {
        CdcCsv cdcCsv = new CdcCsv();
        String[] tokens = s.split(",");
        try {

            cdcCsv.setCounty(tokens[0]);

            if (tokens[1].isBlank())
                cdcCsv.setCounty_fips(-1);
            else
                cdcCsv.setCounty_fips(Integer.valueOf(tokens[1]));

            cdcCsv.setState(tokens[2]);

            if (tokens[3].isBlank())
                cdcCsv.setCounty_population(-1);
            else
                cdcCsv.setCounty_population(Integer.valueOf(tokens[3]));

            if (tokens[4].isBlank())
                cdcCsv.setHealth_service_area_number(-1);
            else
                cdcCsv.setHealth_service_area_number(Integer.valueOf(tokens[4]));

            if (tokens.length > 13) {
                cdcCsv.setHealth_service_area(tokens[5] + tokens[6] + tokens[7]);

                if (tokens[8].isBlank())
                    cdcCsv.setHealth_service_area_population(-1);
                else
                    cdcCsv.setHealth_service_area_population(Integer.valueOf(tokens[8]));

                if (tokens[9].isBlank())
                    cdcCsv.setCovid_inpatient_bed_utilization(-1);
                else
                    cdcCsv.setCovid_inpatient_bed_utilization(Double.valueOf(tokens[9]));

                if (tokens[10].isBlank())
                    cdcCsv.setCovid_hospital_admissions_per_100k(-1);
                else
                    cdcCsv.setCovid_hospital_admissions_per_100k(Double.valueOf(tokens[10]));

                if (tokens[11].isBlank())
                    cdcCsv.setCovid_cases_per_100k(-1);
                else
                    cdcCsv.setCovid_cases_per_100k(Double.valueOf(tokens[11]));

                cdcCsv.setCovid19_community_level(tokens[12]);
                cdcCsv.setDate_updated(tokens[13]);
            } else if (tokens.length == 13) {
                cdcCsv.setHealth_service_area(tokens[5] + tokens[6]);

                if (tokens[7].isBlank())
                    cdcCsv.setHealth_service_area_population(-1);
                else
                    cdcCsv.setHealth_service_area_population(Integer.valueOf(tokens[7]));

                if (tokens[8].isBlank())
                    cdcCsv.setCovid_inpatient_bed_utilization(-1);
                else
                    cdcCsv.setCovid_inpatient_bed_utilization(Double.valueOf(tokens[8]));

                if (tokens[9].isBlank())
                    cdcCsv.setCovid_hospital_admissions_per_100k(-1);
                else
                    cdcCsv.setCovid_hospital_admissions_per_100k(Double.valueOf(tokens[9]));

                if (tokens[10].isBlank())
                    cdcCsv.setCovid_cases_per_100k(-1);
                else
                    cdcCsv.setCovid_cases_per_100k(Double.valueOf(tokens[10]));

                cdcCsv.setCovid19_community_level(tokens[11]);
                cdcCsv.setDate_updated(tokens[12]);
            } else {
                cdcCsv.setHealth_service_area(tokens[5]);
                if (tokens[6].isBlank())
                    cdcCsv.setHealth_service_area_population(-1);
                else
                    cdcCsv.setHealth_service_area_population(Integer.valueOf(tokens[6]));

                if (tokens[7].isBlank())
                    cdcCsv.setCovid_inpatient_bed_utilization(-1);
                else
                    cdcCsv.setCovid_inpatient_bed_utilization(Double.valueOf(tokens[7]));

                if (tokens[8].isBlank())
                    cdcCsv.setCovid_hospital_admissions_per_100k(-1);
                else
                    cdcCsv.setCovid_hospital_admissions_per_100k(Double.valueOf(tokens[8]));

                if (tokens[9].isBlank())
                    cdcCsv.setCovid_cases_per_100k(-1);
                else
                    cdcCsv.setCovid_cases_per_100k(Double.valueOf(tokens[9]));

                cdcCsv.setCovid19_community_level(tokens[10]);
                cdcCsv.setDate_updated(tokens[11]);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return cdcCsv;
    };

    /**
     * @return the county
     */
    public String getCounty() {
        return county;
    }

    /**
     * @param county the county to set
     */
    public void setCounty(String county) {
        this.county = county;
    }

    /**
     * @return the county_fips
     */
    public int getCounty_fips() {
        return county_fips;
    }

    /**
     * @param county_fips the county_fips to set
     */
    public void setCounty_fips(int county_fips) {
        this.county_fips = county_fips;
    }

    /**
     * @return the state
     */
    public String getState() {
        return state;
    }

    /**
     * @param state the state to set
     */
    public void setState(String state) {
        this.state = state;
    }

    /**
     * @return the county_population
     */
    public int getCounty_population() {
        return county_population;
    }

    /**
     * @param county_population the county_population to set
     */
    public void setCounty_population(int county_population) {
        this.county_population = county_population;
    }

    /**
     * @return the health_service_area_number
     */
    public int getHealth_service_area_number() {
        return health_service_area_number;
    }

    /**
     * @param health_service_area_number the health_service_area_number to set
     */
    public void setHealth_service_area_number(int health_service_area_number) {
        this.health_service_area_number = health_service_area_number;
    }

    /**
     * @return the health_service_area
     */
    public String getHealth_service_area() {
        return health_service_area;
    }

    /**
     * @param health_service_area the health_service_area to set
     */
    public void setHealth_service_area(String health_service_area) {
        this.health_service_area = health_service_area;
    }

    /**
     * @return the health_service_area_population
     */
    public int getHealth_service_area_population() {
        return health_service_area_population;
    }

    /**
     * @param health_service_area_population the health_service_area_population to set
     */
    public void setHealth_service_area_population(int health_service_area_population) {
        this.health_service_area_population = health_service_area_population;
    }

    /**
     * @return the covid_inpatient_bed_utilization
     */
    public double getCovid_inpatient_bed_utilization() {
        return covid_inpatient_bed_utilization;
    }

    /**
     * @param covid_inpatient_bed_utilization the covid_inpatient_bed_utilization to set
     */
    public void setCovid_inpatient_bed_utilization(double covid_inpatient_bed_utilization) {
        this.covid_inpatient_bed_utilization = covid_inpatient_bed_utilization;
    }

    /**
     * @return the covid_hospital_admissions_per_100k
     */
    public double getCovid_hospital_admissions_per_100k() {
        return covid_hospital_admissions_per_100k;
    }

    /**
     * @param covid_hospital_admissions_per_100k the covid_hospital_admissions_per_100k to set
     */
    public void setCovid_hospital_admissions_per_100k(double covid_hospital_admissions_per_100k) {
        this.covid_hospital_admissions_per_100k = covid_hospital_admissions_per_100k;
    }

    /**
     * @return the covid_cases_per_100k
     */
    public double getCovid_cases_per_100k() {
        return covid_cases_per_100k;
    }

    /**
     * @param covid_cases_per_100k the covid_cases_per_100k to set
     */
    public void setCovid_cases_per_100k(double covid_cases_per_100k) {
        this.covid_cases_per_100k = covid_cases_per_100k;
    }

    /**
     * @return the covid19_community_level
     */
    public String getCovid19_community_level() {
        return covid19_community_level;
    }

    /**
     * @param covid19_community_level the covid19_community_level to set
     */
    public void setCovid19_community_level(String covid19_community_level) {
        this.covid19_community_level = covid19_community_level;
    }

    /**
     * @return the date_updated
     */
    public String getDate_updated() {
        return date_updated;
    }

    /**
     * @param date_updated the date_updated to set
     */
    public void setDate_updated(String date_updated) {
        this.date_updated = date_updated;
    }

}
