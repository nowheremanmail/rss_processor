package com.dag.news.feeds.bing;

import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "_type",
        "readLink",
        "totalEstimatedMatches",
        "value"
})
public class BingNewsAnswer {

    @JsonProperty("_type")
    private String type;
    @JsonProperty("readLink")
    private String readLink;
    @JsonProperty("totalEstimatedMatches")
    private int totalEstimatedMatches;
    @JsonProperty("value")
    private List<NewsArticle> value = null;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("_type")
    public String getType() {
        return type;
    }

    @JsonProperty("_type")
    public void setType(String type) {
        this.type = type;
    }

    @JsonProperty("readLink")
    public String getReadLink() {
        return readLink;
    }

    @JsonProperty("readLink")
    public void setReadLink(String readLink) {
        this.readLink = readLink;
    }

    @JsonProperty("totalEstimatedMatches")
    public int getTotalEstimatedMatches() {
        return totalEstimatedMatches;
    }

    @JsonProperty("totalEstimatedMatches")
    public void setTotalEstimatedMatches(int totalEstimatedMatches) {
        this.totalEstimatedMatches = totalEstimatedMatches;
    }

    @JsonProperty("value")
    public List<NewsArticle> getValue() {
        return value;
    }

    @JsonProperty("value")
    public void setValue(List<NewsArticle> value) {
        this.value = value;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

}
