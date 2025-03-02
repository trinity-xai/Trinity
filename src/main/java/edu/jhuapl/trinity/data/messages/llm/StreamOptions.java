package edu.jhuapl.trinity.data.messages.llm;

/**
 *
 * @author phillsm1
 */
public class StreamOptions {
    private boolean include_usage = false;
    public StreamOptions() {

    }    

    /**
     * @return the include_usage
     */
    public boolean isInclude_usage() {
        return include_usage;
    }

    /**
     * @param include_usage the include_usage to set
     */
    public void setInclude_usage(boolean include_usage) {
        this.include_usage = include_usage;
    }
}
