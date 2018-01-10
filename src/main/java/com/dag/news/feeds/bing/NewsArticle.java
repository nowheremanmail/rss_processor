package com.dag.news.feeds.bing;

import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "about",
        "provider",
        "datePublished",
        "clusteredArticles",
        "mentions",
        "video",
        "category",
        "name",
        "url",
        "description",
        "image"
})
public class NewsArticle {

    @JsonProperty("about")
    private List<About> about = null;
    @JsonProperty("provider")
    private List<Provider> provider = null;
    @JsonProperty("datePublished")
    private String datePublished;
    @JsonProperty("clusteredArticles")
    private List<NewsArticle> clusteredArticles = null;
    @JsonProperty("mentions")
    private List<Thing> mentions = null;
    @JsonProperty("video")
    private Object video;
    @JsonProperty("category")
    private Object category;
    @JsonProperty("name")
    private String name;
    @JsonProperty("url")
    private String url;
    @JsonProperty("description")
    private String description;
    @JsonProperty("image")
    private Object image;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("about")
    public List<About> getAbout() {
        return about;
    }

    @JsonProperty("about")
    public void setAbout(List<About> about) {
        this.about = about;
    }

    @JsonProperty("provider")
    public List<Provider> getProvider() {
        return provider;
    }

    @JsonProperty("provider")
    public void setProvider(List<Provider> provider) {
        this.provider = provider;
    }

    @JsonProperty("datePublished")
    public String getDatePublished() {
        return datePublished;
    }

    @JsonProperty("datePublished")
    public void setDatePublished(String datePublished) {
        this.datePublished = datePublished;
    }

    @JsonProperty("clusteredArticles")
    public List<NewsArticle> getClusteredArticles() {
        return clusteredArticles;
    }

    @JsonProperty("clusteredArticles")
    public void setClusteredArticles(List<NewsArticle> clusteredArticles) {
        this.clusteredArticles = clusteredArticles;
    }

    @JsonProperty("mentions")
    public List<Thing> getMentions() {
        return mentions;
    }

    @JsonProperty("mentions")
    public void setMentions(List<Thing> mentions) {
        this.mentions = mentions;
    }

    @JsonProperty("video")
    public Object getVideo() {
        return video;
    }

    @JsonProperty("video")
    public void setVideo(Object video) {
        this.video = video;
    }

    @JsonProperty("category")
    public Object getCategory() {
        return category;
    }

    @JsonProperty("category")
    public void setCategory(Object category) {
        this.category = category;
    }

    @JsonProperty("name")
    public String getName() {
        return name;
    }

    @JsonProperty("name")
    public void setName(String name) {
        this.name = name;
    }

    @JsonProperty("url")
    public String getUrl() {
        return url;
    }

    @JsonProperty("url")
    public void setUrl(String url) {
        this.url = url;
    }

    @JsonProperty("description")
    public String getDescription() {
        return description;
    }

    @JsonProperty("description")
    public void setDescription(String description) {
        this.description = description;
    }

    @JsonProperty("image")
    public Object getImage() {
        return image;
    }

    @JsonProperty("image")
    public void setImage(Object image) {
        this.image = image;
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
