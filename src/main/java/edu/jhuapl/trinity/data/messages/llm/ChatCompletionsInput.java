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

    public static final String TYPESTRING = "chatCompletionsInput";
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
    private double frequency_penalty;
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
        stop = new ArrayList<>();
        stream_options = new StreamOptions();
        tools = new ArrayList<>();
        bad_words = new ArrayList<>();
        bad_words.add("string");
    }

    public static ChatCompletionsInput defaultChatCompletionsInput() {
        ChatCompletionsInput input = new ChatCompletionsInput();
        ChatMessage msg = new ChatMessage();
        msg.setRole("user");
        msg.setContent("This is an example message");
        input.getMessages().add(msg);
        input.setModel("meta-llama/Llama-3.2-90B-Vision-Instruct");
        input.setFrequency_penalty(0);
        //default logit_bias object should be sufficient
        input.setLogprobs(false);
        input.setMax_tokens(150);
        input.setN(1);
        input.setPresence_penalty(0);
        //default response_format object should be sufficient
        input.setSeed(-1);
        input.getStop().add("string");
        input.setStream(false);
        //default stream_options object should be sufficient
        input.setTemperature(1);
        input.setTop_p(1);
        //default tools object should be sufficient
        input.setTool_choice("string");
        input.setUser("");
        input.setMin_tokens(1);
        //derfault bad_words object should be sufficient
        input.setTop_k(10);
        return input;
    }
    
    //<editor-fold defaultstate="collapsed" desc="Properties">
    /**
     * @return the messages
     */
    public List<ChatMessage> getMessages() {
        return messages;
    }

    /**
     * @param messages the messages to set
     */
    public void setMessages(List<ChatMessage> messages) {
        this.messages = messages;
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
     * @return the frequency_penalty
     */
    public double getFrequency_penalty() {
        return frequency_penalty;
    }

    /**
     * @param frequency_penalty the frequency_penalty to set
     */
    public void setFrequency_penalty(double frequency_penalty) {
        this.frequency_penalty = frequency_penalty;
    }

    /**
     * @return the logit_bias
     */
    public LogitBias getLogit_bias() {
        return logit_bias;
    }

    /**
     * @param logit_bias the logit_bias to set
     */
    public void setLogit_bias(LogitBias logit_bias) {
        this.logit_bias = logit_bias;
    }

    /**
     * @return the logprobs
     */
    public boolean isLogprobs() {
        return logprobs;
    }

    /**
     * @param logprobs the logprobs to set
     */
    public void setLogprobs(boolean logprobs) {
        this.logprobs = logprobs;
    }

    /**
     * @return the top_logprobs
     */
    public double getTop_logprobs() {
        return top_logprobs;
    }

    /**
     * @param top_logprobs the top_logprobs to set
     */
    public void setTop_logprobs(double top_logprobs) {
        this.top_logprobs = top_logprobs;
    }

    /**
     * @return the max_tokens
     */
    public int getMax_tokens() {
        return max_tokens;
    }

    /**
     * @param max_tokens the max_tokens to set
     */
    public void setMax_tokens(int max_tokens) {
        this.max_tokens = max_tokens;
    }

    /**
     * @return the n
     */
    public int getN() {
        return n;
    }

    /**
     * @param n the n to set
     */
    public void setN(int n) {
        this.n = n;
    }

    /**
     * @return the presence_penalty
     */
    public double getPresence_penalty() {
        return presence_penalty;
    }

    /**
     * @param presence_penalty the presence_penalty to set
     */
    public void setPresence_penalty(double presence_penalty) {
        this.presence_penalty = presence_penalty;
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

    /**
     * @return the seed
     */
    public long getSeed() {
        return seed;
    }

    /**
     * @param seed the seed to set
     */
    public void setSeed(long seed) {
        this.seed = seed;
    }

    /**
     * @return the stop
     */
    public List<String> getStop() {
        return stop;
    }

    /**
     * @param stop the stop to set
     */
    public void setStop(List<String> stop) {
        this.stop = stop;
    }

    /**
     * @return the stream
     */
    public boolean isStream() {
        return stream;
    }

    /**
     * @param stream the stream to set
     */
    public void setStream(boolean stream) {
        this.stream = stream;
    }

    /**
     * @return the stream_options
     */
    public StreamOptions getStream_options() {
        return stream_options;
    }

    /**
     * @param stream_options the stream_options to set
     */
    public void setStream_options(StreamOptions stream_options) {
        this.stream_options = stream_options;
    }

    /**
     * @return the temperature
     */
    public double getTemperature() {
        return temperature;
    }

    /**
     * @param temperature the temperature to set
     */
    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    /**
     * @return the top_p
     */
    public double getTop_p() {
        return top_p;
    }

    /**
     * @param top_p the top_p to set
     */
    public void setTop_p(double top_p) {
        this.top_p = top_p;
    }

    /**
     * @return the tools
     */
    public List<String> getTools() {
        return tools;
    }

    /**
     * @param tools the tools to set
     */
    public void setTools(List<String> tools) {
        this.tools = tools;
    }

    /**
     * @return the tool_choice
     */
    public String getTool_choice() {
        return tool_choice;
    }

    /**
     * @param tool_choice the tool_choice to set
     */
    public void setTool_choice(String tool_choice) {
        this.tool_choice = tool_choice;
    }

    /**
     * @return the user
     */
    public String getUser() {
        return user;
    }

    /**
     * @param user the user to set
     */
    public void setUser(String user) {
        this.user = user;
    }

    /**
     * @return the min_tokens
     */
    public int getMin_tokens() {
        return min_tokens;
    }

    /**
     * @param min_tokens the min_tokens to set
     */
    public void setMin_tokens(int min_tokens) {
        this.min_tokens = min_tokens;
    }

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
     * @return the top_k
     */
    public int getTop_k() {
        return top_k;
    }

    /**
     * @param top_k the top_k to set
     */
    public void setTop_k(int top_k) {
        this.top_k = top_k;
    }
    
    //</editor-fold>    
}
