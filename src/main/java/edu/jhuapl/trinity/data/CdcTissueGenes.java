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

import java.util.ArrayList;
import java.util.function.Function;

/**
 * @author Sean Phillips
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CdcTissueGenes {
    //number	Tissue_name	source	ENSG00000157227	ENSG00000102226	ENSG00000133027	ENSG00000135269	ENSG00000175727	ENSG00000179776	ENSG00000125967	ENSG00000275832	ENSG00000000971	ENSG00000169116	ENSG00000169926	ENSG00000161013	ENSG00000064300	ENSG00000108829	ENSG00000144635
    //0	Blood	GTEx	1.075874867	3.293370484	2.339992484	5.100977648	3.681449265	0.15704371	2.195032565	0.232292362	0.539828811	0.634964929	3.960697039	4.008092421	1.704429352	3.504620392	4.498889087
    //1	Blood	GTEx	0.413810687	2.201006466	0.917775441	3.237104773	4.130107179	0.084554154	1.393965276	0.810072871	0.278341659	0.509138416	3.502075956	3.221722443	0.119953718	3.081169167	3.339992484
    //2	Blood	GTEx	2.317593505	3.529820947	2.757236647	5.711219557	4.720825666	0.590817563	2.101986173	0.673194574	1.911499849	0.588900488	4.797012978	4.266036894	0.414893223	4.685379552	5.104755987

    private int number;
    private String tissueName;
    private String source;
    private ArrayList<Double> genes;

    public static Function<String, CdcTissueGenes> csvToCdcTissueGenes = s -> {
        CdcTissueGenes cdcTissueGenes = new CdcTissueGenes();
        String[] tokens = s.split(",");
        try {
            cdcTissueGenes.setNumber(Integer.parseInt(tokens[0]));
            cdcTissueGenes.setTissueName(tokens[1]);
            //cdcTissueGenes.setSource(tokens[2]); //latest version does NOT have source
            //everything after must be a gene. Could be variable amount of columns
            cdcTissueGenes.genes = new ArrayList<>(tokens.length - 2);
            //-1 because they accidentally duplicated tissue_name in last column
            for (int i = 3; i < tokens.length - 1; i++) {
                cdcTissueGenes.genes.add(Double.valueOf(tokens[i]));
            }
        } catch (NumberFormatException ex) {
            ex.printStackTrace();
        }
        return cdcTissueGenes;
    };

    public CdcTissueGenes() {

    }

    /**
     * @return the number
     */
    public int getNumber() {
        return number;
    }

    /**
     * @param number the number to set
     */
    public void setNumber(int number) {
        this.number = number;
    }

    /**
     * @return the tissueName
     */
    public String getTissueName() {
        return tissueName;
    }

    /**
     * @param tissueName the tissueName to set
     */
    public void setTissueName(String tissueName) {
        this.tissueName = tissueName;
    }

    /**
     * @return the source
     */
    public String getSource() {
        return source;
    }

    /**
     * @param source the source to set
     */
    public void setSource(String source) {
        this.source = source;
    }

    /**
     * @return the genes
     */
    public ArrayList<Double> getGenes() {
        return genes;
    }

    /**
     * @param genes the genes to set
     */
    public void setGenes(ArrayList<Double> genes) {
        this.genes = genes;
    }


}
