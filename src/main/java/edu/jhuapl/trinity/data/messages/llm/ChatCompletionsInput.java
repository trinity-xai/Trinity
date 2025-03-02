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
public class ChatCompletionsInput {

    public static final String TYPESTRING = "embeddingImageInput";
    public static final String BASE64_PREFIX_PNG = "data:image/png;base64,";
    //<editor-fold defaultstate="collapsed" desc="JSON Payload">
    /*
        {
          "messages": [
            {
              "role": "user",
              "content": "This is an example message"
            }
          ],
          "model": "meta-llama/Llama-3.2-90B-Vision-Instruct",
          "frequency_penalty": 0,
          "logit_bias": {},
          "logprobs": false,
          "top_logprobs": 0,
          "max_tokens": 150,
          "n": 1,
          "presence_penalty": 0,
          "response_format": {
            "type": "text",
            "json_schema": {},
            "regex": "string",
            "multiple_choice": [
              "string"
            ]
          },
          "seed": -1,
          "stop": [
            "string"
          ],
          "stream": false,
          "stream_options": {
            "include_usage": false
          },
          "temperature": 1,
          "top_p": 1,
          "tools": [],
          "tool_choice": "string",
          "user": "",
          "min_tokens": 1,
          "bad_words": [
            "string"
          ],
          "top_k": 10
        }
    
     */
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Payload Fields">
    private List<ChatMessage> messages;
    private String model; 
    private String frequency_penalty;
    private LogitBias logit_bias;
    private boolean logprobs;
    private double top_logprobs;
    private int max_tokens;
    private int n;
    private double presence_penalty;
    private ResponseFormat response_format;
    private long seed;
    private List<String> stop;
    private boolean stream;
    private StreamOptions stream_options;
    private double temperature;
    private double top_p;
    private List<String> tools;
    private String tool_choice;
    private String user;
    private int min_tokens;
    private List<String> bad_words;
    private int top_k;
    //</editor-fold>

    public ChatCompletionsInput() {
        messages = new ArrayList<>();
        response_format = new ResponseFormat();
        logit_bias = new LogitBias();
        tools = new ArrayList<>();
        bad_words = new ArrayList<>();
        bad_words.add("string");
        
    }

    public class LogitBias {
        public LogitBias(){
        }
    }
    
    //<editor-fold defaultstate="collapsed" desc="Properties">

    //</editor-fold>    
}
