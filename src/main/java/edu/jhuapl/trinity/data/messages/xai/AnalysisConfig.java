package edu.jhuapl.trinity.data.messages.xai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.jhuapl.trinity.data.messages.MessageData;

import java.util.ArrayList;
import java.util.List;


/**
 * @author Sean Phillips
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AnalysisConfig extends MessageData {

    public static final String TYPESTRING = "analysis_config";
    public static String DEFAULT_PREFIX = "AnalysisConfig-";
    //<editor-fold defaultstate="collapsed" desc="JSON Payload">
    /*
TBD SMP
    */
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Payload Fields">

    private String analysisName = null;
    private String umapConfigFile = null;
    private String contentBasePath = null;
    private String notes = "";
    private List<String> dataSources;

    //</editor-fold>

    public AnalysisConfig() {
        messageType = TYPESTRING;
        dataSources = new ArrayList<>();

    }

    public static boolean isAnalysisConfig(String messageBody) {
        return messageBody.contains("messageType")
            && messageBody.contains(TYPESTRING)
            && messageBody.contains("analysisName")
            && messageBody.contains("umapConfigFile");
    }

    public String prettyPrint() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(this);
    }

    public String print() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper.writeValueAsString(this);
    }
    //<editor-fold defaultstate="collapsed" desc="Properties">

    /**
     * @return the analysisName
     */
    public String getAnalysisName() {
        return analysisName;
    }

    /**
     * @param analysisName the analysisName to set
     */
    public void setAnalysisName(String analysisName) {
        this.analysisName = analysisName;
    }

    /**
     * @return the umapConfigFile
     */
    public String getUmapConfigFile() {
        return umapConfigFile;
    }

    /**
     * @param umapConfigFile the umapConfigFile to set
     */
    public void setUmapConfigFile(String umapConfigFile) {
        this.umapConfigFile = umapConfigFile;
    }

    /**
     * @return the notes
     */
    public String getNotes() {
        return notes;
    }

    /**
     * @param notes the notes to set
     */
    public void setNotes(String notes) {
        this.notes = notes;
    }

    /**
     * @return the dataSources
     */
    public List<String> getDataSources() {
        return dataSources;
    }

    /**
     * @param dataSources the dataSources to set
     */
    public void setDataSources(List<String> dataSources) {
        this.dataSources = dataSources;
    }

    /**
     * @return the contentBasePath
     */
    public String getContentBasePath() {
        return contentBasePath;
    }

    /**
     * @param contentBasePath the contentBasePath to set
     */
    public void setContentBasePath(String contentBasePath) {
        this.contentBasePath = contentBasePath;
    }

    //</editor-fold>
}
