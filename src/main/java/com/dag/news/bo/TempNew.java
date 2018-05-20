package com.dag.news.bo;

import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TempNew implements Serializable {
    @JsonIgnore
    private String source;

    private List<String> categories;
    private String title;
    private String description;
    private String link;

    private Date date;

    @JsonIgnore
    private String language;

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


    public String getDay() {
        return day;
    }


    public void setDay(String day) {
        this.day = day;
    }

    @JsonIgnore
    private String day;

    public void setDate(Date pubDate) {

        this.date = pubDate;

        SimpleDateFormat sdt = new SimpleDateFormat("yyyyMMdd");

        day = sdt.format(pubDate);
    }

    public String getLanguage() {
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
    }

    public TempNew(String source, String language, String title, String description, String link, Date pubDate, List<String> categories) {
        this.title = title;
        this.description = description;
        this.link = link;
        this.setDate(pubDate);

        this.language = language;
        this.source = source;
        this.categories = categories;
    }

    @Override
    public String toString() {
        return "TempNew{" +
                "source='" + source + '\'' +
                ", categories=" + categories +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", link='" + link + '\'' +
                ", date=" + date +
                ", language='" + language + '\'' +
                ", day='" + day + '\'' +
                '}';
    }
}