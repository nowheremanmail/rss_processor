package com.dag.news.bo;

import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Date;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TempNew {
    //private String source;
    private List<String> categories;
    private String title;
    private String description;
    private String link;
    private Date date;
    //private String language;

    public TempNew() {
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public List<String> getCategories() {
        return categories;
    }

    public void setCategories(List<String> categories) {
        this.categories = categories;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date pubDate) {
        this.date = pubDate;
    }

    /*public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }*/

    public TempNew(String title, String description, String link, Date pubDate, List<String> categories) {
        this.title = title;
        this.description = description;
        this.link = link;
        this.date = pubDate;
        //this.language = language;
        //this.source = source;
        this.categories = categories;
    }

}