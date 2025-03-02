package edu.jhuapl.trinity.data.messages.llm;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author phillsm1
 */
public class ResponseFormat {
    private String type = "text";
    private JsonSchema json_schema;
    private String regex = "string";
    private List<String> multiple_choice;

    public ResponseFormat() {
        multiple_choice = new ArrayList<>();
        multiple_choice.add("string");
        json_schema = new JsonSchema();
    }

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * @return the json_schema
     */
    public JsonSchema getJson_schema() {
        return json_schema;
    }

    /**
     * @param json_schema the json_schema to set
     */
    public void setJson_schema(JsonSchema json_schema) {
        this.json_schema = json_schema;
    }

    /**
     * @return the regex
     */
    public String getRegex() {
        return regex;
    }

    /**
     * @param regex the regex to set
     */
    public void setRegex(String regex) {
        this.regex = regex;
    }

    /**
     * @return the multiple_choice
     */
    public List<String> getMultiple_choice() {
        return multiple_choice;
    }

    /**
     * @param multiple_choice the multiple_choice to set
     */
    public void setMultiple_choice(List<String> multiple_choice) {
        this.multiple_choice = multiple_choice;
    }
    public class JsonSchema {
        public JsonSchema(){
        }
    }    
}
