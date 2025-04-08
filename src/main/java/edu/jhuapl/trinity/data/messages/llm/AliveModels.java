package edu.jhuapl.trinity.data.messages.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Sean Phillips
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AliveModels {
    //<editor-fold defaultstate="collapsed" desc="JSON Payload">
    /*
        {
            "alive_models":[
                {"id":"intfloat/multilingual-e5-large"},
                {"id":"intfloat/e5-large-v2"},
                {"id":"BAAI/bge-large-en-v1.5"},
                {"id":"BAAI/bge-small-en-v1.5"},
                {"id":"WhereIsAI/UAE-Large-V1"},
                {"id":"openai/clip-vit-large-patch14"}
            ]
        }
     */
    //</editor-fold>

    private List<AiModel> alive_models;

    public AliveModels() {
        alive_models = new ArrayList<>();
    }

    /**
     * @return the alive_models
     */
    public List<AiModel> getAlive_models() {
        return alive_models;
    }

    /**
     * @param alive_models the alive_models to set
     */
    public void setAlive_models(List<AiModel> alive_models) {
        this.alive_models = alive_models;
    }

}
