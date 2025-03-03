package edu.jhuapl.trinity.data.messages.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Sean Phillips
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatGenerationParameters {
    
    /*
      "generation_parameters": {
        "bad_words": null,
        "stop": null,
        "output_length": 150,
        "top_k": 10,
        "top_p": 1,
        "temperature": 0,
        "frequency_penalty": 0,
        "presence_penalty": 0,
        "seed": -1,
        "start_id": null,
        "end_id": null,
        "response_format": null
      }    

    */    
    private List<String> bad_words;
    private String stop;
    private Integer output_length;
    private Integer top_k;
    private Integer top_p;
    private Double temperature;
    private Double frequency_penalty;
    private Double presence_penalty;
    private Long seed;
    private String start_id;
    private String end_id;
    private ResponseFormat response_format;
    
    public ChatGenerationParameters() {
        bad_words = new ArrayList<>();
    }
    //<editor-fold defaultstate="collapsed" desc="Properties">

    /**
     * @return the bad_words
     */
    public List<String> getBad_words() {
        return bad_words;
    }

    /**
     * @param bad_words the bad_words to set
     */
    public void setBad_words(List<String> bad_words) {
        this.bad_words = bad_words;
    }

    /**
     * @return the stop
     */
    public String getStop() {
        return stop;
    }

    /**
     * @param stop the stop to set
     */
    public void setStop(String stop) {
        this.stop = stop;
    }

    /**
     * @return the output_length
     */
    public Integer getOutput_length() {
        return output_length;
    }

    /**
     * @param output_length the output_length to set
     */
    public void setOutput_length(Integer output_length) {
        this.output_length = output_length;
    }

    /**
     * @return the top_k
     */
    public Integer getTop_k() {
        return top_k;
    }

    /**
     * @param top_k the top_k to set
     */
    public void setTop_k(Integer top_k) {
        this.top_k = top_k;
    }

    /**
     * @return the top_p
     */
    public Integer getTop_p() {
        return top_p;
    }

    /**
     * @param top_p the top_p to set
     */
    public void setTop_p(Integer top_p) {
        this.top_p = top_p;
    }

    /**
     * @return the temperature
     */
    public Double getTemperature() {
        return temperature;
    }

    /**
     * @param temperature the temperature to set
     */
    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    /**
     * @return the frequency_penalty
     */
    public Double getFrequency_penalty() {
        return frequency_penalty;
    }

    /**
     * @param frequency_penalty the frequency_penalty to set
     */
    public void setFrequency_penalty(Double frequency_penalty) {
        this.frequency_penalty = frequency_penalty;
    }

    /**
     * @return the presence_penalty
     */
    public Double getPresence_penalty() {
        return presence_penalty;
    }

    /**
     * @param presence_penalty the presence_penalty to set
     */
    public void setPresence_penalty(Double presence_penalty) {
        this.presence_penalty = presence_penalty;
    }

    /**
     * @return the seed
     */
    public Long getSeed() {
        return seed;
    }

    /**
     * @param seed the seed to set
     */
    public void setSeed(Long seed) {
        this.seed = seed;
    }

    /**
     * @return the start_id
     */
    public String getStart_id() {
        return start_id;
    }

    /**
     * @param start_id the start_id to set
     */
    public void setStart_id(String start_id) {
        this.start_id = start_id;
    }

    /**
     * @return the end_id
     */
    public String getEnd_id() {
        return end_id;
    }

    /**
     * @param end_id the end_id to set
     */
    public void setEnd_id(String end_id) {
        this.end_id = end_id;
    }

    /**
     * @return the response_format
     */
    public ResponseFormat getResponse_format() {
        return response_format;
    }

    /**
     * @param response_format the response_format to set
     */
    public void setResponse_format(ResponseFormat response_format) {
        this.response_format = response_format;
    }
//</editor-fold>
}
