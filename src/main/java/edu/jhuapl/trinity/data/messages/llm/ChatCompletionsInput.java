package edu.jhuapl.trinity.data.messages.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import static edu.jhuapl.trinity.data.messages.llm.ImageUrl.BASE64_PREFIX_PNG;
import edu.jhuapl.trinity.utils.ResourceUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.image.Image;

/**
 * @author Sean Phillips
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatCompletionsInput {

    public static final String TYPESTRING = "chatCompletionsInput";
    public static enum CAPTION_TYPE { DEFAULT, AUTOCHOOOSE }
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
    private Double frequency_penalty;
    private LogitBias logit_bias;
    private Boolean logprobs;
    private Double top_logprobs;
    private Integer max_tokens;
    private Integer n;
    private Double presence_penalty;
    private ResponseFormat response_format;
    private Long seed;
    private List<String> stop;
    private Boolean stream;
    private StreamOptions stream_options;
    private Double temperature;
    private Double top_p;
    private List<String> tools;
    private String tool_choice;
    private String user;
    private Integer min_tokens;
    private List<String> bad_words;
    private Integer top_k;
    //</editor-fold>

    public ChatCompletionsInput() {
        messages = new ArrayList<>();
//Leave null as they are unnecessary 99% of the time
//        response_format = new ResponseFormat();
//        logit_bias = new LogitBias();
//        stop = new ArrayList<>();
//        stream_options = new StreamOptions();
//        tools = new ArrayList<>();
//        bad_words = new ArrayList<>();
//        bad_words.add("string");
    }
    public static ChatCompletionsInput hellocarlChatCompletionsInput() throws IOException {
        Image image = ResourceUtils.load3DTextureImage("carl-b-portrait");
        return defaultImageInput(image);
    }          
    public static ChatCompletionsInput defaultImageInput(Image image) throws IOException {
        ImageUrl imageUrl = new ImageUrl();
        imageUrl.setUrl(BASE64_PREFIX_PNG + ResourceUtils.imageToBase64(image));
        return defaultImageInput(imageUrl, CAPTION_TYPE.DEFAULT);        
    }
    public static ChatCompletionsInput defaultImageInput(ImageUrl imageUrl, CAPTION_TYPE type) throws IOException {
        /*    
        {
            "model": "meta-llama/Llama-3.2-90B-Vision-Instruct",
            "messages": [
              {
                "role": "user",
                "content": [
                  {
                    "type": "text",
                    "text": "What is in this image?"
                  },
                  {
                    "type": "image_url",
                    "image_url": {
                      "url": "https://upload.wikimedia.org/wikipedia/commons/thumb/d/dd/Gfp-wisconsin-madison-the-nature-boardwalk.jpg/2560px-Gfp-wisconsin-madison-the-nature-boardwalk.jpg"
                    }
                  }
                ]
              }
            ]
          }
        */
        ChatCompletionsInput input = new ChatCompletionsInput();
        ChatMessage msg = new ChatMessage();
        msg.setRole("user");
        Content promptContent = new Content();
        promptContent.setTypeByEnum(Content.TYPE_ENUM.text);
        String promptString = Prompts.loadDefaultCaptionPrompt();
        if(type == CAPTION_TYPE.AUTOCHOOOSE)
            promptString = Prompts.loadAutochooseCaptionPrompt();
        promptContent.setText(promptString);
        msg.addContent(promptContent);
        Content imageContent = new Content();
        imageContent.setTypeByEnum(Content.TYPE_ENUM.image_url);
        imageContent.setImage_url(imageUrl);
        msg.addContent(imageContent);
        input.getMessages().add(msg);
        input.setModel("meta-llama/Llama-3.2-90B-Vision-Instruct");
        input.setMax_tokens(300);
        input.setTemperature(1.0);
        return input;             
    }
  
    public static ChatCompletionsInput helloworldChatCompletionsInput() {
        /*    
        {
          "messages": [
            {
              "role": "user",
              "content": "Say hello in 3 languages"
            }
          ],
          "model": "meta-llama/Llama-3.2-90B-Vision-Instruct",
          "max_tokens": 150,
          "temperature": 0.0
        }
        */
        ChatCompletionsInput input = new ChatCompletionsInput();
        ChatMessage msg = new ChatMessage();
        msg.setRole("user");
        Content content = new Content();
        content.setTypeByEnum(Content.TYPE_ENUM.text);
        content.setText("Say Hello in 3 languages. You choose the languages.");
        msg.addContent(content);
        input.getMessages().add(msg);
        input.setModel("meta-llama/Llama-3.2-90B-Vision-Instruct");
        input.setMax_tokens(150);
        input.setTemperature(1.0);
        return input;
    }
    public static ChatCompletionsInput defaultFullChatCompletionsInput() {
        ChatCompletionsInput input = new ChatCompletionsInput();
        ChatMessage msg = new ChatMessage();
        msg.setRole("user");
        Content content = new Content();
        content.setTypeByEnum(Content.TYPE_ENUM.text);
        content.setText("This is an example message");
        msg.addContent(content);
        input.getMessages().add(msg);
        input.setModel("meta-llama/Llama-3.2-90B-Vision-Instruct");
        input.setFrequency_penalty(0.0);
        //default logit_bias object should be sufficient
        input.setLogprobs(false);
        input.setMax_tokens(150);
        input.setN(1);
        input.setPresence_penalty(0.0);
        //default response_format object should be sufficient
        input.setSeed(-1L);
        input.getStop().add("string");
        input.setStream(false);
        //default stream_options object should be sufficient
        input.setTemperature(1.0);
        input.setTop_p(1.0);
        //default tools object should be sufficient
        input.setTool_choice("string");
        input.setUser("");
        input.setMin_tokens(1);
        //derfault bad_words object should be sufficient
        input.setTop_k(10);
        return input;
    }
    
    //<editor-fold defaultstate="collapsed" desc="Properties">

    
    //</editor-fold>    

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
    public Boolean getLogprobs() {
        return logprobs;
    }

    /**
     * @param logprobs the logprobs to set
     */
    public void setLogprobs(Boolean logprobs) {
        this.logprobs = logprobs;
    }

    /**
     * @return the top_logprobs
     */
    public Double getTop_logprobs() {
        return top_logprobs;
    }

    /**
     * @param top_logprobs the top_logprobs to set
     */
    public void setTop_logprobs(Double top_logprobs) {
        this.top_logprobs = top_logprobs;
    }

    /**
     * @return the max_tokens
     */
    public Integer getMax_tokens() {
        return max_tokens;
    }

    /**
     * @param max_tokens the max_tokens to set
     */
    public void setMax_tokens(Integer max_tokens) {
        this.max_tokens = max_tokens;
    }

    /**
     * @return the n
     */
    public Integer getN() {
        return n;
    }

    /**
     * @param n the n to set
     */
    public void setN(Integer n) {
        this.n = n;
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
    public Boolean getStream() {
        return stream;
    }

    /**
     * @param stream the stream to set
     */
    public void setStream(Boolean stream) {
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
     * @return the top_p
     */
    public Double getTop_p() {
        return top_p;
    }

    /**
     * @param top_p the top_p to set
     */
    public void setTop_p(Double top_p) {
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
    public Integer getMin_tokens() {
        return min_tokens;
    }

    /**
     * @param min_tokens the min_tokens to set
     */
    public void setMin_tokens(Integer min_tokens) {
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
    public Integer getTop_k() {
        return top_k;
    }

    /**
     * @param top_k the top_k to set
     */
    public void setTop_k(Integer top_k) {
        this.top_k = top_k;
    }
}
