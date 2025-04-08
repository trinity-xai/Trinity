package edu.jhuapl.trinity.data.messages.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import edu.jhuapl.trinity.data.messages.MessageData;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Sean Phillips
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatCompletionsOutput extends MessageData {

    public static final String TYPESTRING = "imgoutput";
    //<editor-fold defaultstate="collapsed" desc="JSON Payload">
    /*
{
  "object": "text_completion",
  "model": "meta-llama/Llama-3.2-90B-Vision-Instruct",
  "prompt": "",
  "choices": [
    {
      "text": "Here are greetings in three languages:\n\n1. English: Hello\n2. Spanish: Hola\n3. French: Bonjour",
      "message": {
        "content": "Here are greetings in three languages:\n\n1. English: Hello\n2. Spanish: Hola\n3. French: Bonjour",
        "role": "assistant",
        "tool_calls": []
      },
      "raw": "Here are greetings in three languages:\n\n1. English: Hello\n2. Spanish: Hola\n3. French: Bonjour",
      "index": 0,
      "logprobs": null,
      "finish_reason": "stop",
      "tool_calls": null
    }
  ],
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
}

    */
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Payload Fields">
    private String object;
    private String model;
    private String prompt;
    private List<ChatCompletionChoice> choices;
    private ChatGenerationParameters generation_parameters;

    //</editor-fold>
    //extra helper fields
    private int inputID;
    private int requestNumber;

    public ChatCompletionsOutput() {
        this.messageType = TYPESTRING;
        choices = new ArrayList<>();
    }

    //<editor-fold defaultstate="collapsed" desc="Properties">

    /**
     * @return the object
     */
    public String getObject() {
        return object;
    }

    /**
     * @param object the object to set
     */
    public void setObject(String object) {
        this.object = object;
    }

    /**
     * @return the model
     */
    public String getModel() {
        return model;
    }

    /**
     * @param model the model to set
     */
    public void setModel(String model) {
        this.model = model;
    }


    /**
     * @return the requestNumber
     */
    public int getRequestNumber() {
        return requestNumber;
    }

    /**
     * @param requestNumber the requestNumber to set
     */
    public void setRequestNumber(int requestNumber) {
        this.requestNumber = requestNumber;
    }

    /**
     * @return the prompt
     */
    public String getPrompt() {
        return prompt;
    }

    /**
     * @param prompt the prompt to set
     */
    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    /**
     * @return the choices
     */
    public List<ChatCompletionChoice> getChoices() {
        return choices;
    }

    /**
     * @param choices the choices to set
     */
    public void setChoices(List<ChatCompletionChoice> choices) {
        this.choices = choices;
    }

    /**
     * @return the generation_parameters
     */
    public ChatGenerationParameters getGeneration_parameters() {
        return generation_parameters;
    }

    /**
     * @param generation_parameters the generation_parameters to set
     */
    public void setGeneration_parameters(ChatGenerationParameters generation_parameters) {
        this.generation_parameters = generation_parameters;
    }
    //</editor-fold>

    /**
     * @return the inputID
     */
    public int getInputID() {
        return inputID;
    }

    /**
     * @param inputID the inputID to set
     */
    public void setInputID(int inputID) {
        this.inputID = inputID;
    }
}
